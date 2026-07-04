package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.order.domain.dto.CouponClaimMessage;
import com.ecommerce.order.domain.po.ECoupon;
import com.ecommerce.order.domain.po.EUserCoupon;
import com.ecommerce.order.domain.vo.CouponVO;
import com.ecommerce.order.domain.vo.UserCouponVO;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.UserCouponStatus;
import com.ecommerce.order.mapper.EUserCouponMapper;
import com.ecommerce.order.service.IECouponService;
import com.ecommerce.order.service.IEUserCouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.COUPON_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.COUPON_CLAIM_KEY;
import static com.ecommerce.order.constants.RedisConstants.COUPON_META_PREFIX;
import static com.ecommerce.order.constants.RedisConstants.COUPON_USER_CLAIMED_PREFIX;
import static com.ecommerce.order.constants.RedisConstants.USER_COUPONS_PREFIX;
import static com.ecommerce.order.constants.RedisConstants.USER_COUPONS_TTL_MINUTES;

@Slf4j
@Service
@RequiredArgsConstructor
public class EUserCouponServiceImpl extends ServiceImpl<EUserCouponMapper, EUserCoupon> implements IEUserCouponService {

    private final IECouponService couponService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;
    private final DefaultRedisScript<Long> claimCouponScript;
    private final ObjectMapper objectMapper;

    @Override
    public R<List<CouponVO>> listAvailable() {
        //获取用户id
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }
        //根据筛选所有启用的优惠卷
        List<ECoupon> coupons = couponService.lambdaQuery()
                .eq(ECoupon::getStatus, CouponStatus.ENABLED)
                .list();

        if (coupons.isEmpty()) {
            return R.ok(Collections.emptyList());
        }

        // pipeline 批量取 coupon:meta:{id} 的 stock 和 status，一次网络往返
        List<Object> pipeResults = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations ops) {
                for (ECoupon c : coupons) {
                    ops.opsForHash().get(COUPON_META_PREFIX + c.getId(), "stock");
                    ops.opsForHash().get(COUPON_META_PREFIX + c.getId(), "status");
                }
                return null;
            }});
        Map<Long, String> stockMap = new HashMap<>();
        Map<Long, String> statusMap = new HashMap<>();
        for (int j = 0; j < coupons.size(); j++) {
            Object stockVal = pipeResults.get(j * 2);
            Object statusVal = pipeResults.get(j * 2 + 1);
            Long cid = coupons.get(j).getId();
            stockMap.put(cid, stockVal instanceof String ? (String) stockVal : null);
            statusMap.put(cid, statusVal instanceof String ? (String) statusVal : null);
        }

        // 批量查用户已领取数量，过滤已达上限的券
        List<String> userClaimedKeys = coupons.stream()
                .map(c -> COUPON_USER_CLAIMED_PREFIX + c.getId() + ":" + userId)
                .collect(Collectors.toList());
        List<String> userClaimedVals = redisTemplate.opsForValue().multiGet(userClaimedKeys);

        List<CouponVO> result = new ArrayList<>();
        for (int i = 0; i < coupons.size(); i++) {
            ECoupon c = coupons.get(i);
            // 库存不足
            int remain;
            String stockStr = stockMap.get(c.getId());
            if (stockStr != null) {
                remain = Integer.parseInt(stockStr);
            } else {
                // 缓存未命中：用 DB 计算剩余量，不写 Redis（初始化交给 claim Lua 原子完成）
                int claimedCount = c.getClaimedCount() != null ? c.getClaimedCount() : 0;
                remain = c.getTotalCount() - claimedCount;
            }
            if (remain <= 0) continue;

            // 缓存显示已停用则隐藏
            String cachedStatus = statusMap.get(c.getId());
            if (cachedStatus != null && Integer.parseInt(cachedStatus) != CouponStatus.ENABLED.getValue()) {
                continue;
            }

            // 用户已达领取上限
            String claimedStr = userClaimedVals.get(i);
            int userClaimed = claimedStr != null ? Integer.parseInt(claimedStr) : 0;
            if (c.getLimitPerUser() != null && userClaimed >= c.getLimitPerUser()) {
                continue;
            }

            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
            vo.setDescription(buildDesc(c));
            result.add(vo);
        }
        return R.ok(result);
    }

    @Override
    public R<Void> claim(Long couponId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }

        ECoupon coupon = couponService.getById(couponId);
        if (coupon == null || coupon.getStatus() != CouponStatus.ENABLED) {
            throw new BadRequestException("优惠券不存在或已停用");
        }

        int claimed = coupon.getClaimedCount() != null ? coupon.getClaimedCount() : 0;
        int validDays = coupon.getValidDays() != null ? coupon.getValidDays() : 30;
        String metaKey = COUPON_META_PREFIX + couponId;
        String userClaimKey = COUPON_USER_CLAIMED_PREFIX + couponId + ":" + userId;
        Long result = redisTemplate.execute(claimCouponScript,
                List.of(metaKey, userClaimKey),
                String.valueOf(coupon.getLimitPerUser()),
                String.valueOf(coupon.getTotalCount() - claimed),
                String.valueOf(CouponStatus.ENABLED.getValue()));

        if (result == null) {
            throw new BadRequestException("领取失败，请稍后重试");
        }
        if (result == -1) {
            throw new BadRequestException("优惠券已被抢光");
        }
        if (result == -2) {
            throw new BadRequestException("已达到该优惠券领取上限");
        }
        if (result == -3) {
            throw new BadRequestException("优惠券已停用");
        }

        // 同步发送 MQ，失败则补偿 Redis
        try {
            rabbitMqHelper.send(COUPON_EXCHANGE, COUPON_CLAIM_KEY,
                    new CouponClaimMessage(userId, couponId, validDays));
        } catch (Exception e) {
            log.error("MQ 发送失败，补偿 Redis: userId={}, couponId={}", userId, couponId, e);
            redisTemplate.opsForHash().increment(metaKey, "stock", 1);
            redisTemplate.opsForValue().decrement(userClaimKey);
            throw new BadRequestException("领取失败，请稍后重试");
        }

        // 删除用户券包缓存
        redisTemplate.delete(USER_COUPONS_PREFIX + userId);
        return R.ok();
    }

    @Override
    public R<Map<String, List<UserCouponVO>>> myCoupons() {
        // 1. 获取当前登录用户
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }

        // 2. 查 Redis 缓存：Hash 结构 key=user:coupons:{userId}，field=userCouponId，value=VO JSON
        String key = USER_COUPONS_PREFIX + userId;
        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);

        // 3. 缓存命中 → 反序列化 + 实时过期修正，直接返回
        if (!cached.isEmpty()) {
            // 过滤 _empty 占位标记（新用户空券包防穿透）
            cached.remove("_empty");
            if (cached.isEmpty()) {
                return R.ok(emptyCouponResult());
            }
            // JSON → VO 列表，实时修正已过期状态，直接返回（模板状态变更时缓存已被清除）
            return R.ok(classifyAndBuildVOs(cached.values()));
        }

        // 4. 缓存未命中 → 查 DB 用户券记录表
        List<EUserCoupon> records = lambdaQuery()
                .eq(EUserCoupon::getUserId, userId)
                .orderByDesc(EUserCoupon::getClaimedAt)
                .list();

        // 5. 收集 couponId，批量查券模板表（1 次 IN 查询替代 N 次单查）
        Set<Long> couponIds = records.stream().map(EUserCoupon::getCouponId).collect(Collectors.toSet());
        //用id批量查询优惠卷实体
        Map<Long, ECoupon> couponMap = Collections.emptyMap();
        if (!couponIds.isEmpty()) {
            couponMap = couponService.listByIds(couponIds).stream()
                    .collect(Collectors.toMap(ECoupon::getId, c -> c));
        }

        // 6. 逐条组装 VO，同步序列化到缓存 Map
        List<UserCouponVO> allVOs = new ArrayList<>();
        Map<String, String> cacheData = new HashMap<>();
        for (EUserCoupon uc : records) {
            ECoupon template = couponMap.get(uc.getCouponId());
            UserCouponVO vo = buildUserCouponVO(uc, template);
            allVOs.add(vo);
            try {
                cacheData.put(uc.getId().toString(), objectMapper.writeValueAsString(vo));
            } catch (Exception ignored) {}
        }

        // 7. 回写 Redis 缓存
        if (cacheData.isEmpty()) {
            // 空结果也写占位标记，TTL 1 分钟，防止新用户频繁穿透 DB
            redisTemplate.opsForHash().put(key, "_empty", "1");
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        } else {
            // 正常结果批量写 Hash，TTL 使用配置值
            redisTemplate.opsForHash().putAll(key, cacheData);
            redisTemplate.expire(key, USER_COUPONS_TTL_MINUTES, TimeUnit.MINUTES);
        }

        // 8. 按状态分桶返回
        return R.ok(classifyVOs(allVOs));
    }

    // ======================== private helpers ========================

    /**
     * 组装 UserCouponVO（用户券记录 + 券模板信息）并判定最终状态
     */
    private UserCouponVO buildUserCouponVO(EUserCoupon uc, ECoupon template) {
        UserCouponVO vo = BeanUtils.copyBean(uc, UserCouponVO.class);
        if (template != null) {
            vo.setCouponName(template.getName());
            vo.setType(template.getType().getValue());
            vo.setThreshold(template.getThreshold());
            vo.setReduce(template.getReduce());
        }
        // 模板停用 → 视为失效；未使用但已过期 → 也视为失效
        if (template != null && template.getStatus() != CouponStatus.ENABLED) {
            vo.setStatus(UserCouponStatus.EXPIRED.getValue());
            vo.setStatusText("已失效");
        } else if (uc.getStatus() == UserCouponStatus.UNUSED
                && uc.getExpireAt() != null
                && uc.getExpireAt().isBefore(LocalDateTime.now())) {
            vo.setStatus(UserCouponStatus.EXPIRED.getValue());
            vo.setStatusText(UserCouponStatus.EXPIRED.getDesc());
        } else {
            vo.setStatus(uc.getStatus().getValue());
            vo.setStatusText(uc.getStatus().getDesc());
        }
        return vo;
    }

    /**
     * Redis Hash 缓存值 → JSON 反序列化 → 状态刷新 → 分类
     */
    private Map<String, List<UserCouponVO>> classifyAndBuildVOs(Collection<Object> cachedValues) {
        List<UserCouponVO> allVOs = new ArrayList<>();
        for (Object obj : cachedValues) {
            try {
                UserCouponVO vo = objectMapper.readValue(obj.toString(), UserCouponVO.class);
                // 缓存中的未使用券可能已过期，实时修正状态
                if (vo.getStatus() != null && vo.getStatus() == UserCouponStatus.UNUSED.getValue()
                        && vo.getExpireAt() != null
                        && vo.getExpireAt().isBefore(LocalDateTime.now())) {
                    vo.setStatus(UserCouponStatus.EXPIRED.getValue());
                    vo.setStatusText(UserCouponStatus.EXPIRED.getDesc());
                }
                allVOs.add(vo);
            } catch (Exception e) {
                log.warn("解析用户优惠券缓存失败", e);
            }
        }
        return classifyVOs(allVOs);
    }

    /**
     * 按状态分桶：available / used / expired
     */
    private Map<String, List<UserCouponVO>> classifyVOs(List<UserCouponVO> allVOs) {
        List<UserCouponVO> available = new ArrayList<>();
        List<UserCouponVO> used = new ArrayList<>();
        List<UserCouponVO> expired = new ArrayList<>();
        for (UserCouponVO vo : allVOs) {
            int status = vo.getStatus() != null ? vo.getStatus() : UserCouponStatus.EXPIRED.getValue();
            if (status == UserCouponStatus.USED.getValue()) {
                used.add(vo);
            } else if (status == UserCouponStatus.EXPIRED.getValue()) {
                expired.add(vo);
            } else {
                available.add(vo);
            }
        }
        Map<String, List<UserCouponVO>> result = new LinkedHashMap<>();
        result.put("available", available);
        result.put("used", used);
        result.put("expired", expired);
        return result;
    }

    /**
     * 拼券描述文案，例如"满100元减20元"、"无门槛立减5元"
     */
    private String buildDesc(ECoupon coupon) {
        double thresholdYuan = coupon.getThreshold() != null ? coupon.getThreshold() / 100.0 : 0;
        double reduceYuan = coupon.getReduce() != null ? coupon.getReduce() / 100.0 : 0;
        if (coupon.getType() == com.ecommerce.order.enums.CouponType.NO_THRESHOLD) {
            return "无门槛立减" + formatYuan(reduceYuan) + "元";
        }
        return "满" + formatYuan(thresholdYuan) + "元减" + formatYuan(reduceYuan) + "元";
    }

    /**
     * 金额格式化：整数不带小数位（10 而非 10.0），小数保留原样
     */
    private String formatYuan(double yuan) {
        if (yuan == (long) yuan) {
            return String.valueOf((long) yuan);
        }
        return String.valueOf(yuan);
    }

    /**
     * 空券包返回结构（available / used / expired 均为空列表）
     */
    private Map<String, List<UserCouponVO>> emptyCouponResult() {
        Map<String, List<UserCouponVO>> result = new LinkedHashMap<>();
        result.put("available", Collections.emptyList());
        result.put("used", Collections.emptyList());
        result.put("expired", Collections.emptyList());
        return result;
    }
}
