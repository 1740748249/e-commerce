package com.ecommerce.product.job;

import cn.hutool.core.util.IdUtil;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.enums.FlashSaleOrderStatus;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_RESTORE_KEY;
import static com.ecommerce.product.constants.CacheConstants.FLASH_STOCK_PREFIX;
import static com.ecommerce.product.constants.CacheConstants.FLASH_USER_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleTimeoutJob {

    private final IEFlashSaleOrderService flashSaleOrderService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> flashCompensatedRollbackScript;
    private final RabbitMqHelper rabbitMqHelper;
    private final OrderClient orderClient;

    @Transactional
    @XxlJob("flashSaleTimeoutCancel")
    public void cancelTimeout() {
        log.info("秒杀超时取消任务开始");
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        LocalDateTime cancelTime = LocalDateTime.now();

        // 1. 查询超时待支付订单（LIMIT 1000 防 OOM）
        List<EFlashSaleOrder> timeoutOrders = flashSaleOrderService.lambdaQuery()
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .le(EFlashSaleOrder::getCreateTime, deadline)
                .last("LIMIT 1000")
                .list();

        if (timeoutOrders.isEmpty()) {
            log.info("秒杀超时取消任务结束，无超时订单");
            return;
        }

        Set<Long> ids = timeoutOrders.stream()
                .map(EFlashSaleOrder::getId)
                .collect(Collectors.toSet());

        // 2. 批量更新，WHERE status = PENDING_PAYMENT 作为乐观锁防支付竞态
        flashSaleOrderService.lambdaUpdate()
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAYMENT_TIMEOUT)
                .set(EFlashSaleOrder::getCancelTime, cancelTime)
                .in(EFlashSaleOrder::getId, ids)
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .update();

        // 3. 事务内回查：只取本次批量更新实际生效的订单（支付回调已改状态的不会出现）
        List<EFlashSaleOrder> cancelled = flashSaleOrderService.lambdaQuery()
                .in(EFlashSaleOrder::getId, ids)
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAYMENT_TIMEOUT)
                .list();

        if (cancelled.isEmpty()) {
            log.info("秒杀超时取消任务结束，所有订单已被支付");
            return;
        }

        // 4. 对已取消的订单回补 Redis 库存（补偿脚本原子检查标记+回补，防竞态）
        for (EFlashSaleOrder order : cancelled) {
            try {
                Long result = redisTemplate.execute(flashCompensatedRollbackScript,
                        List.of(FLASH_STOCK_PREFIX + order.getFlashSaleId(),
                                FLASH_USER_PREFIX + order.getFlashSaleId(),
                                "flash:compensated:" + order.getId()),
                        String.valueOf(order.getUserId()),
                        String.valueOf(order.getQuantity()),
                        String.valueOf(Duration.ofHours(24).getSeconds()));
                if (result != null && result == 1) {
                    // 异步回补 DB 库存
                    rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_RESTORE_KEY,
                            FlashStockSyncMessage.builder()
                                    .messageId(IdUtil.fastSimpleUUID())
                                    .flashSaleId(order.getFlashSaleId())
                                    .quantity(order.getQuantity())
                                    .build());
                }
                // 清理订单缓存，防止 result() 返回旧状态
                redisTemplate.delete("flash:order:" + order.getFlashSaleId() + ":" + order.getUserId());

                // 同步取消 EOrder，防止用户继续支付已释放库存的秒杀订单
                try {
                    R<Void> cancelR = orderClient.cancelOrderByTimeout(order.getOrderNo());
                    if (!cancelR.success()) {
                        log.warn("同步取消 EOrder 返回非成功: orderNo={}", order.getOrderNo());
                    }
                } catch (Exception feignEx) {
                    log.error("同步取消 EOrder 失败（库存已回补不受影响）: orderNo={}", order.getOrderNo(), feignEx);
                }
            } catch (Exception e) {
                log.error("超时取消回补Redis失败 orderId={} flashSaleId={} userId={} quantity={}",
                        order.getId(), order.getFlashSaleId(), order.getUserId(), order.getQuantity(), e);
            }
        }

        log.info("秒杀超时取消任务结束，取消 {} 笔订单", cancelled.size());
    }
}
