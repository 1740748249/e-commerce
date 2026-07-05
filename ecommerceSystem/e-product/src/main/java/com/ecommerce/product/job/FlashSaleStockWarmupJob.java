package com.ecommerce.product.job;

import cn.hutool.json.JSONUtil;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.domain.po.EFlashSession;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.service.IEFlashSaleService;
import com.ecommerce.product.service.IEFlashSessionService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecommerce.product.constants.CacheConstants.*;

/**
 * 秒杀库存预热 — 场次开始前将秒杀库存、SKU 快照加载到 Redis，避免秒杀时回源 DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleStockWarmupJob {

    private final IEFlashSessionService flashSessionService;
    private final IEFlashSaleService flashSaleService;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("flashSaleStockWarmup")
    public void warmup() {
        log.info("秒杀库存预热任务开始");
        LocalDateTime now = LocalDateTime.now();                    // 当前时间
        LocalDateTime threshold = now.plusMinutes(5);               // 提前5分钟窗口

        // 1. 查未来5分钟内即将开始的场次
        List<EFlashSession> sessions = flashSessionService.lambdaQuery()
                .ge(EFlashSession::getStartTime, now)                // 开始时间 >= 当前
                .le(EFlashSession::getStartTime, threshold)          // 开始时间 <= 5分钟后
                .list();

        if (sessions.isEmpty()) {
            log.info("秒杀库存预热任务结束，无需预热的场次");
            return;
        }

        // 2. 收集场次ID集合 + 建 sessionId→SessionObj 快速查找表
        Set<Long> sessionIds = sessions.stream()
                .map(EFlashSession::getId)
                .collect(Collectors.toSet());
        Map<Long, EFlashSession> sessionMap = sessions.stream()
                .collect(Collectors.toMap(EFlashSession::getId, s -> s));

        // 3. 一次 SQL 查出所有场次下已审批通过的秒杀商品
        List<EFlashSale> items = flashSaleService.lambdaQuery()
                .in(EFlashSale::getSessionId, sessionIds)             // 属于目标场次
                .eq(EFlashSale::getApprovalStatus, ApprovalStatus.APPROVED) // 已审批
                .list();

        if (CollUtils.isEmpty(items)) {
            log.info("秒杀库存预热任务结束，无秒杀商品");
            return;
        }

        // 4. 按场次分组，逐个写入Redis，同一场次只预热一次
        Map<Long, List<EFlashSale>> grouped = items.stream()
                .collect(Collectors.groupingBy(EFlashSale::getSessionId)); // sessionId → [秒杀商品列表]
        int warmed = 0;                                              // 计数器：本次实际预热了几个场次

        for (Map.Entry<Long, List<EFlashSale>> entry : grouped.entrySet()) {
            Long sid = entry.getKey();                               // 场次ID
            List<EFlashSale> sessionItems = entry.getValue();       // 该场次下的秒杀商品
            EFlashSession session = sessionMap.get(sid);            // 场次详情

            // 若已预热过（标记键存在），直接跳过
            String warmedKey = FLASH_WARMED_PREFIX + sid;           // 标记键：flash:warmed:{sessionId}
            if (Boolean.TRUE.equals(redisTemplate.hasKey(warmedKey))) {
                continue;                                            // 已预热，跳过
            }

            // TTL = 场次结束时间 - 当前 + 1小时缓冲，过期自动清理
            long ttl = ChronoUnit.SECONDS.between(now, session.getEndTime()) + 3600;
            redisTemplate.executePipelined(new SessionCallback<Object>() { // Pipeline 批量写入，一次网络往返
                @Override
                public Object execute(RedisOperations operations) {
                    for (EFlashSale item : sessionItems) {
                        // 旧数据无 skuId，跳过（不再在 Redis 中预热该 SKU）
                        if (item.getSkuId() == null) {
                            log.warn("秒杀活动缺少 skuId，已跳过预热: flashSaleId={}", item.getId());
                            continue;
                        }
                        // ① 仅场次未开始时清除旧购买记录（防止上一轮残留）
                        if (now.isBefore(session.getStartTime())) {
                            operations.delete(FLASH_USER_PREFIX + item.getId()); // flash:user:{saleId}
                        }
                        // ② 写入秒杀库存 → flash:stock:{saleId}
                        operations.opsForValue().set(
                                FLASH_STOCK_PREFIX + item.getId(),
                                String.valueOf(item.getStock()),
                                Duration.ofSeconds(ttl));
                        // ③ 写入 SKU ID → flash:sku:{saleId}（下单时用）
                        operations.opsForValue().set(
                                FLASH_SKU_PREFIX + item.getId(),
                                String.valueOf(item.getSkuId()),
                                Duration.ofSeconds(ttl));
                        // ④ 写入秒杀活动快照 → flash:sale:{saleId}（order() 优先读缓存，避免回源DB）
                        operations.opsForValue().set(
                                FLASH_SALE_PREFIX + item.getId(),
                                JSONUtil.toJsonStr(item),
                                Duration.ofSeconds(ttl));
                    }
                    // ⑤ 写入场次已预热标记 → flash:warmed:{sessionId}，防止重复预热
                    operations.opsForValue().set(warmedKey, "1", Duration.ofSeconds(ttl));
                    return null;
                }
            });
            warmed++;                                                // 计数+1
        }

        log.info("秒杀库存预热任务结束，{} 个场次中预热 {} 个（已跳过已预热）", grouped.size(), warmed);
    }
}
