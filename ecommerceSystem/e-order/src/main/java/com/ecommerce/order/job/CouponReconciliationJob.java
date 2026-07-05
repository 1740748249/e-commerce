package com.ecommerce.order.job;

import com.ecommerce.order.domain.po.ECoupon;
import com.ecommerce.order.enums.CouponStatus;
import com.ecommerce.order.service.IECouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecommerce.order.constants.RedisConstants.COUPON_META_PREFIX;

/**
 * 优惠券 Redis ↔ DB 双向库存对账。
 * - Redis 有值且不一致 → Redis 为实时守门员，以 Redis 修正 DB
 * - Redis 无值（宕机重启等）→ DB 为持久化记录，以 DB 回填 Redis
 *
 * XXL-JOB 建议 Cron：0 0 3 * * ?（每天凌晨 3 点）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReconciliationJob {

    private final IECouponService couponService;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("couponStockReconciliation")
    public void reconcile() {
        log.info("优惠券库存对账开始");

        List<ECoupon> coupons = couponService.lambdaQuery()
                .eq(ECoupon::getStatus, CouponStatus.ENABLED)
                .list();

        if (coupons.isEmpty()) {
            log.info("无启用的优惠券，对账结束");
            return;
        }

        // 1. Pipeline 批量读 Redis stock + status（一次网络往返）
        List<Object> pipeResults = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("rawtypes")
            public Object execute(RedisOperations ops) {
                for (ECoupon c : coupons) {
                    ops.opsForHash().get(COUPON_META_PREFIX + c.getId(), "stock");
                    ops.opsForHash().get(COUPON_META_PREFIX + c.getId(), "status");
                }
                return null;
            }
        });

        // 2. 内存对比 + 批量修正
        int fixedDb = 0;     // Redis → DB 方向
        int fixedRedis = 0;  // DB → Redis 方向

        for (int i = 0; i < coupons.size(); i++) {
            ECoupon coupon = coupons.get(i);
            int dbClaimed = coupon.getClaimedCount() != null ? coupon.getClaimedCount() : 0;
            int dbStock = coupon.getTotalCount() - dbClaimed;

            Object stockVal = i * 2 < pipeResults.size() ? pipeResults.get(i * 2) : null;
            int redisStock;
            try {
                redisStock = stockVal != null ? Integer.parseInt(stockVal.toString()) : -1;
            } catch (NumberFormatException e) {
                redisStock = -1;
            }

            if (redisStock < 0) {
                // ===== Redis 无数据 → DB 回填 Redis =====
                log.warn("Redis 缓存缺失: couponId={}, 以 DB 回填, stock={}", coupon.getId(), dbStock);
                String metaKey = COUPON_META_PREFIX + coupon.getId();
                redisTemplate.opsForHash().put(metaKey, "stock", String.valueOf(dbStock));
                redisTemplate.opsForHash().put(metaKey, "status",
                        String.valueOf(CouponStatus.ENABLED.getValue()));
                fixedRedis++;
                continue;
            }

            int expectedDbClaimed = coupon.getTotalCount() - redisStock;
            if (dbClaimed != expectedDbClaimed) {
                // ===== Redis 有数据且不一致 → Redis 修正 DB =====
                log.warn("DB 库存不一致: couponId={}, RedisStock={}, DB claimedCount={} → 修正为 {}",
                        coupon.getId(), redisStock, dbClaimed, expectedDbClaimed);
                couponService.lambdaUpdate()
                        .eq(ECoupon::getId, coupon.getId())
                        .set(ECoupon::getClaimedCount, expectedDbClaimed)
                        .update();
                fixedDb++;
            }
        }

        log.info("优惠券库存对账结束: 共 {} 张, DB修正 {} 张, Redis回填 {} 张",
                coupons.size(), fixedDb, fixedRedis);
    }
}
