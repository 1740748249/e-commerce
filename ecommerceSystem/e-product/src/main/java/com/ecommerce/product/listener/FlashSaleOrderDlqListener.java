package com.ecommerce.product.listener;

import cn.hutool.core.util.IdUtil;
import com.ecommerce.api.message.FlashSaleOrderMessage;
import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.product.config.FlashSaleDlqConfig;
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
public class FlashSaleOrderDlqListener {

    private final IEFlashSaleOrderService flashSaleOrderService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> flashCompensatedRollbackScript;
    private final RabbitMqHelper rabbitMqHelper;

    @RabbitListener(queues = FlashSaleDlqConfig.FLASH_ORDER_DLQ)
    public void onDlq(FlashSaleOrderMessage msg) {
        try {
            doCompensate(msg);
        } catch (Exception e) {
            log.error("秒杀订单死信补偿异常，等待超时任务兜底: flashSaleOrderId={}",
                    msg.getFlashSaleOrderId(), e);
        }
    }

    private void doCompensate(FlashSaleOrderMessage msg) {
        log.warn("秒杀订单创建死信，开始补偿: flashSaleOrderId={}", msg.getFlashSaleOrderId());

        EFlashSaleOrder order = flashSaleOrderService.getById(msg.getFlashSaleOrderId());
        if (order == null) {
            log.info("闪购订单不存在，跳过: flashSaleOrderId={}", msg.getFlashSaleOrderId());
            return;
        }
        if (order.getStatus() != FlashSaleOrderStatus.PENDING_PAYMENT) {
            log.info("订单状态已变更，跳过: flashSaleOrderId={}, status={}",
                    msg.getFlashSaleOrderId(), order.getStatus());
            return;
        }

        // 先更新 DB，乐观锁 eq(status, PENDING_PAYMENT) 防支付并发
        // 谁先改成功谁赢：支付赢了就不再回补，取消赢了就不让支付
        boolean updated = flashSaleOrderService.lambdaUpdate()
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAYMENT_TIMEOUT)
                .set(EFlashSaleOrder::getCancelTime, LocalDateTime.now())
                .eq(EFlashSaleOrder::getId, msg.getFlashSaleOrderId())
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .update();

        if (!updated) {
            log.info("订单状态已被支付回调修改，放弃补偿: flashSaleOrderId={}",
                    msg.getFlashSaleOrderId());
            return;
        }

        // DB 确认已取消，再回补 Redis
        Long result = redisTemplate.execute(flashCompensatedRollbackScript,
                List.of(FLASH_STOCK_PREFIX + msg.getFlashSaleId(),
                        FLASH_USER_PREFIX + msg.getFlashSaleId(),
                        "flash:compensated:" + msg.getFlashSaleOrderId()),
                String.valueOf(msg.getUserId()),
                String.valueOf(msg.getQuantity()),
                String.valueOf(Duration.ofHours(24).getSeconds()));

        if (result != null && result == 1) {
            // 异步回补 DB 库存，与 Redis 保持一致
            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_RESTORE_KEY,
                    FlashStockSyncMessage.builder()
                            .messageId(IdUtil.fastSimpleUUID())
                            .flashSaleId(msg.getFlashSaleId())
                            .quantity(msg.getQuantity())
                            .build());
        } else {
            log.info("Redis 已被补偿过（超时任务可能已处理）: flashSaleOrderId={}",
                    msg.getFlashSaleOrderId());
        }

        redisTemplate.delete(FLASH_ORDER_PREFIX + msg.getFlashSaleId() + ":" + msg.getUserId());
        log.info("秒杀订单死信补偿完成: flashSaleOrderId={}", msg.getFlashSaleOrderId());
    }
}
