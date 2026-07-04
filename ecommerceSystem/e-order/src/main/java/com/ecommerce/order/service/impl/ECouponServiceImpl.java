package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.DbException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.StringUtils;
import com.ecommerce.order.domain.dto.CouponCreateDTO;
import com.ecommerce.order.domain.po.ECoupon;
import com.ecommerce.order.query.CouponPageQuery;
import com.ecommerce.order.domain.vo.CouponVO;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.CouponType;
import com.ecommerce.order.mapper.ECouponMapper;
import com.ecommerce.order.service.IECouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecommerce.order.constants.RedisConstants.COUPON_META_PREFIX;
import static com.ecommerce.order.constants.RedisConstants.USER_COUPONS_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class ECouponServiceImpl extends ServiceImpl<ECouponMapper, ECoupon> implements IECouponService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> initCouponMetaScript;

    @Override
    public R<PageDTO<CouponVO>> page(CouponPageQuery query) {
        Page<ECoupon> mpPage = lambdaQuery()
                .eq(query.getStatus() != null, ECoupon::getStatus, query.getStatus())
                .eq(query.getType() != null, ECoupon::getType, query.getType())
                .like(StringUtils.isNotBlank(query.getKeyword()), ECoupon::getName, query.getKeyword())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ECoupon> records = mpPage.getRecords();
        if(CollUtils.isEmpty(records)){
            return R.ok(PageDTO.of(mpPage,CollUtils.emptyList()));
        }
        List<CouponVO> voList = records.stream().map(item -> {
            CouponVO couponVO = BeanUtils.copyBean(item, CouponVO.class);
            couponVO.setDescription(buildDesc(item));
            return couponVO;
        }).collect(Collectors.toList());
        return R.ok(PageDTO.of(mpPage,voList));
    }

    @Override
    public R<CouponVO> detail(Long id) {
        ECoupon coupon = getById(id);
        if (coupon == null) {
            return R.error("优惠券不存在");
        }
        CouponVO couponVO = BeanUtils.copyBean(coupon, CouponVO.class);
        couponVO.setDescription(buildDesc(coupon));
        return R.ok(couponVO);
    }

    @Override
    @Transactional
    public R<CouponVO> create(CouponCreateDTO dto) {
        if (dto.getType() == CouponType.FULL_REDUCTION.getValue()
                && (dto.getThreshold() == null || dto.getThreshold() <= 0)) {
            throw new BadRequestException("满减券必须设置使用门槛");
        }
        ECoupon coupon = BeanUtils.copyBean(dto, ECoupon.class);
        coupon.setType(CouponType.of(dto.getType()));
        coupon.setStatus(CouponStatus.ENABLED);
        coupon.setClaimedCount(0);
        boolean save = save(coupon);
        if(!save){
            throw new DbException("新增优惠卷失败");
        }
        redisTemplate.execute(initCouponMetaScript,
                List.of(COUPON_META_PREFIX + coupon.getId()),
                String.valueOf(coupon.getTotalCount()),
                String.valueOf(CouponStatus.ENABLED.getValue()));
        CouponVO vo = BeanUtils.copyBean(coupon, CouponVO.class);
        vo.setClaimedCount(0);
        return R.ok(vo);
    }

    @Override
    @Transactional
    public R<Void> update(Long id, CouponCreateDTO dto) {
        ECoupon coupon = getById(id);
        if (coupon == null) {
            return R.error("优惠券不存在");
        }
        if (dto.getType() != null && dto.getType() == CouponType.FULL_REDUCTION.getValue()
                && (dto.getThreshold() == null || dto.getThreshold() <= 0)) {
            throw new BadRequestException("满减券必须设置使用门槛");
        }
        int oldTotal = coupon.getTotalCount();
        boolean stockChanged = dto.getTotalCount() != null
                && !dto.getTotalCount().equals(oldTotal);
        BeanUtils.copyProperties(dto, coupon);
        if (dto.getType() != null) {
            coupon.setType(CouponType.of(dto.getType()));
        }
        updateById(coupon);
        if (stockChanged && coupon.getStatus() == CouponStatus.ENABLED) {
            int delta = coupon.getTotalCount() - oldTotal;
            redisTemplate.opsForHash().increment(COUPON_META_PREFIX + id, "stock", delta);
        }
        // 模板变更（名称/门槛/金额/有效期等）→ 清用户券包缓存，避免展示旧值
        clearUserCouponCaches();
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> updateStatus(Long id, Integer status) {
        ECoupon coupon = getById(id);
        if (coupon == null) {
            return R.error("优惠券不存在");
        }
        CouponStatus target = CouponStatus.of(status);
        if (target == CouponStatus.ENABLED) {
            // 启用：先重建 Redis → 立即可领 → 再落 DB
            redisTemplate.execute(initCouponMetaScript,
                    List.of(COUPON_META_PREFIX + id),
                    String.valueOf(coupon.getTotalCount() - coupon.getClaimedCount()),
                    String.valueOf(CouponStatus.ENABLED.getValue()));
        } else {
            // 停用：先掐 Redis → 立即阻断领券 → 再落 DB（HSET 原子，无需 Lua）
            redisTemplate.opsForHash().put(COUPON_META_PREFIX + id, "status",
                    String.valueOf(CouponStatus.DISABLED.getValue()));
        }
        coupon.setStatus(target);
        updateById(coupon);
        clearUserCouponCaches();
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> delete(Long id) {
        ECoupon coupon = getById(id);
        if (coupon == null) {
            return R.error("优惠券不存在");
        }
        removeById(id);
        redisTemplate.delete(COUPON_META_PREFIX + id);
        return R.ok();
    }

    private void clearUserCouponCaches() {
        Set<String> keysToRemove = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(USER_COUPONS_PREFIX + "*").count(100).build())) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
            }
            return keys;
        });
        if (keysToRemove != null && !keysToRemove.isEmpty()) {
            redisTemplate.unlink(keysToRemove);
        }
    }

    private String buildDesc(ECoupon coupon) {
        double thresholdYuan = coupon.getThreshold() != null ? coupon.getThreshold() / 100.0 : 0;
        double reduceYuan = coupon.getReduce() != null ? coupon.getReduce() / 100.0 : 0;
        if (coupon.getType() == CouponType.NO_THRESHOLD) {
            return "无门槛立减" + formatYuan(reduceYuan) + "元";
        }
        return "满" + formatYuan(thresholdYuan) + "元减" + formatYuan(reduceYuan) + "元";
    }

    private String formatYuan(double yuan) {
        if (yuan == (long) yuan) {
            return String.valueOf((long) yuan);
        }
        return String.valueOf(yuan);
    }
}
