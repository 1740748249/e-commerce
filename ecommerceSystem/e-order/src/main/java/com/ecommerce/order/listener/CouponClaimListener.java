package com.ecommerce.order.listener;

import com.ecommerce.order.domain.dto.CouponClaimMessage;
import com.ecommerce.order.domain.po.ECoupon;
import com.ecommerce.order.domain.po.EUserCoupon;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.enums.UserCouponStatus;
import com.ecommerce.order.mapper.EUserCouponMapper;
import com.ecommerce.order.service.IECouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.ecommerce.order.constants.MqConstants.COUPON_CLAIM_QUEUE;
import static com.ecommerce.order.constants.RedisConstants.COUPON_META_PREFIX;
import static com.ecommerce.order.constants.RedisConstants.COUPON_USER_CLAIMED_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponClaimListener extends ServiceImpl<EUserCouponMapper, EUserCoupon> {

    private final IECouponService couponService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = COUPON_CLAIM_QUEUE)
    @Transactional
    public void onClaim(CouponClaimMessage msg) {
        log.debug("处理优惠券领券落库: userId={}, couponId={}", msg.getUserId(), msg.getCouponId());

        // 幂等检查：同一用户+同一券只处理一次
        long existCount = lambdaQuery()
                .eq(EUserCoupon::getUserId, msg.getUserId())
                .eq(EUserCoupon::getCouponId, msg.getCouponId())
                .count();
        if (existCount > 0) {
            log.info("重复消息，跳过: userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
            return;
        }

        // 乐观锁防超发 + 状态兜底：库存和状态双重校验，任一不满足则更新失败自动补偿
        boolean updated = couponService.lambdaUpdate()
                .eq(ECoupon::getId, msg.getCouponId())
                .eq(ECoupon::getStatus, CouponStatus.ENABLED)
                .apply("claimed_count < total_count")
                .setSql("claimed_count = claimed_count + 1")
                .update();
        if (!updated) {
            // 极少发生（Redis Lua 已限流），补偿 Redis
            log.warn("优惠券 DB 乐观锁失败，补偿 Redis: userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
            redisTemplate.opsForHash().increment(COUPON_META_PREFIX + msg.getCouponId(), "stock", 1);
            redisTemplate.opsForValue().decrement(COUPON_USER_CLAIMED_PREFIX + msg.getCouponId() + ":" + msg.getUserId());
            return;
        }

        EUserCoupon userCoupon = new EUserCoupon();
        userCoupon.setUserId(msg.getUserId());
        userCoupon.setCouponId(msg.getCouponId());
        userCoupon.setStatus(UserCouponStatus.UNUSED);
        userCoupon.setClaimedAt(LocalDateTime.now());
        userCoupon.setExpireAt(LocalDateTime.now().plusDays(msg.getValidDays()));
        save(userCoupon);
    }
}
