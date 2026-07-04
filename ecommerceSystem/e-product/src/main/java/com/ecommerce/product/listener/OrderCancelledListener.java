package com.ecommerce.product.listener;

import cn.hutool.core.util.IdUtil;
import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.api.message.OrderCancelledMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.enums.FlashSaleOrderStatus;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_RESTORE_KEY;
import static com.ecommerce.product.constants.CacheConstants.FLASH_ORDER_PREFIX;
import static com.ecommerce.product.constants.CacheConstants.FLASH_STOCK_PREFIX;
import static com.ecommerce.product.constants.CacheConstants.FLASH_USER_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledListener {

    private final IEFlashSaleOrderService flashSaleOrderService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> flashCompensatedRollbackScript;
    private final RabbitMqHelper rabbitMqHelper;

    @RabbitListener(queues = "order.cancelled.notify.queue")
    public void onOrderCancelled(OrderCancelledMessage msg) {
        EFlashSaleOrder flashOrder = flashSaleOrderService.lambdaQuery()
                .eq(EFlashSaleOrder::getOrderNo, msg.getOrderNo())
                .one();
        if (flashOrder == null) {
            log.debug("非秒杀订单，跳过: orderNo={}", msg.getOrderNo());
            return;
        }
        if (flashOrder.getStatus() != FlashSaleOrderStatus.PENDING_PAYMENT) {
            log.info("秒杀订单状态已变更，跳过: orderNo={}, status={}",
                    msg.getOrderNo(), flashOrder.getStatus());
            return;
        }

        // 乐观锁更新 DB，防支付并发
        boolean updated = flashSaleOrderService.lambdaUpdate()
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.CANCELLED)
                .set(EFlashSaleOrder::getCancelTime, LocalDateTime.now())
                .eq(EFlashSaleOrder::getId, flashOrder.getId())
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .update();
        if (!updated) {
            log.info("秒杀订单已被支付回调修改，放弃补偿: orderNo={}", msg.getOrderNo());
            return;
        }

        // Redis 回补秒杀库存 + 用户已购计数
        Long result = redisTemplate.execute(flashCompensatedRollbackScript,
                List.of(FLASH_STOCK_PREFIX + flashOrder.getFlashSaleId(),
                        FLASH_USER_PREFIX + flashOrder.getFlashSaleId(),
                        "flash:compensated:" + flashOrder.getId()),
                String.valueOf(flashOrder.getUserId()),
                String.valueOf(flashOrder.getQuantity()),
                String.valueOf(Duration.ofHours(24).getSeconds()));

        if (result != null && result == 1) {
            // 异步回补 DB 库存，与 Redis 保持一致
            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_RESTORE_KEY,
                    FlashStockSyncMessage.builder()
                            .messageId(IdUtil.fastSimpleUUID())
                            .flashSaleId(flashOrder.getFlashSaleId())
                            .quantity(flashOrder.getQuantity())
                            .build());
        } else {
            log.info("Redis 已被补偿过: flashSaleOrderId={}", flashOrder.getId());
        }

        redisTemplate.delete(FLASH_ORDER_PREFIX + flashOrder.getFlashSaleId() + ":" + flashOrder.getUserId());
        log.info("秒杀订单取消补偿完成: orderNo={}, flashSaleOrderId={}", msg.getOrderNo(), flashOrder.getId());
    }

    // 退款回补秒杀库存（不涉及用户已购计数，仅 INCR 库存 + 异步落 DB）
    @RabbitListener(queues = "flash.sale.refund.queue")
    public void onFlashSaleRefund(OrderCancelledMessage msg) {
        EFlashSaleOrder flashOrder = flashSaleOrderService.lambdaQuery()
                .eq(EFlashSaleOrder::getOrderNo, msg.getOrderNo())
                .one();
        if (flashOrder == null) {
            log.debug("非秒杀订单，跳过: orderNo={}", msg.getOrderNo());
            return;
        }
        if (flashOrder.getStatus() == FlashSaleOrderStatus.REFUNDED) {
            log.info("秒杀订单已退款，跳过: orderNo={}", msg.getOrderNo());
            return;
        }

        // 幂等：防 MQ 重复投递导致库存重复回补
        String dedupKey = "flash:refund:" + flashOrder.getId();
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofHours(24));
        if (absent == null || !absent) {
            log.info("退款消息重复，跳过: flashSaleOrderId={}", flashOrder.getId());
            return;
        }

        // Redis 先回补库存（Redis 是库存事实来源，先于 DB 执行）
        redisTemplate.opsForValue().increment(FLASH_STOCK_PREFIX + flashOrder.getFlashSaleId(),
                flashOrder.getQuantity());

        // 乐观锁更新 DB 状态为已退款（DB 作为对账基准）
        boolean updated = flashSaleOrderService.lambdaUpdate()
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.REFUNDED)
                .eq(EFlashSaleOrder::getId, flashOrder.getId())
                .ne(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.REFUNDED)
                .update();
        if (!updated) {
            log.info("秒杀订单退款DB状态已被处理: orderNo={}", msg.getOrderNo());
        }

        // 异步回补 DB 库存
        rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_RESTORE_KEY,
                FlashStockSyncMessage.builder()
                        .messageId(IdUtil.fastSimpleUUID())
                        .flashSaleId(flashOrder.getFlashSaleId())
                        .quantity(flashOrder.getQuantity())
                        .build());

        redisTemplate.delete(FLASH_ORDER_PREFIX + flashOrder.getFlashSaleId() + ":" + flashOrder.getUserId());
        log.info("秒杀退款库存回补完成: orderNo={}, flashSaleOrderId={}", msg.getOrderNo(), flashOrder.getId());
    }
}
