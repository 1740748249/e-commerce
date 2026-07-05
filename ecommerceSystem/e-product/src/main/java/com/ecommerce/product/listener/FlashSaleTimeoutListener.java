package com.ecommerce.product.listener;

import cn.hutool.core.util.IdUtil;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.product.domain.dto.FlashSaleTimeoutMessage;
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
import java.util.List;

import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_RESTORE_KEY;
import static com.ecommerce.product.constants.CacheConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleTimeoutListener {

    private final IEFlashSaleOrderService flashSaleOrderService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> flashCompensatedRollbackScript;
    private final RabbitMqHelper rabbitMqHelper;
    private final OrderClient orderClient;

    @RabbitListener(queues = "flash.timeout.delay.queue")
    public void onFlashSaleTimeout(FlashSaleTimeoutMessage msg) {
        log.info("秒杀订单超时检查: flashSaleOrderId={}", msg.getFlashSaleOrderId());

        // 乐观锁更新 EFlashSaleOrder，防支付竞态
        boolean updated = flashSaleOrderService.lambdaUpdate()
                .eq(EFlashSaleOrder::getId, msg.getFlashSaleOrderId())
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAYMENT_TIMEOUT)
                .update();
        if (!updated) {
            log.info("秒杀订单已支付或已取消，跳过: flashSaleOrderId={}", msg.getFlashSaleOrderId());
            return;
        }

        // Lua 补偿回补 Redis 库存（自带幂等标记防重复回补）
        Long result = redisTemplate.execute(flashCompensatedRollbackScript,
                List.of(FLASH_STOCK_PREFIX + msg.getFlashSaleId(),
                        FLASH_USER_PREFIX + msg.getFlashSaleId(),
                        "flash:compensated:" + msg.getFlashSaleOrderId()),
                String.valueOf(msg.getUserId()),
                String.valueOf(msg.getQuantity()),
                String.valueOf(Duration.ofHours(24).getSeconds()));

        if (result != null && result == 1) {
            // MQ 异步回补 DB 库存
            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_RESTORE_KEY,
                    FlashStockSyncMessage.builder()
                            .messageId(IdUtil.fastSimpleUUID())
                            .flashSaleId(msg.getFlashSaleId())
                            .quantity(msg.getQuantity())
                            .build());
        }

        // 清理订单缓存
        redisTemplate.delete(FLASH_ORDER_PREFIX + msg.getFlashSaleId() + ":" + msg.getUserId());

        // Feign 同步取消 EOrder
        try {
            orderClient.cancelOrderByTimeout(msg.getOrderNo());
        } catch (Exception e) {
            log.error("同步取消 EOrder 失败（库存已回补不受影响）: orderNo={}", msg.getOrderNo(), e);
        }

        log.info("秒杀订单超时取消完成: flashSaleOrderId={}", msg.getFlashSaleOrderId());
    }
}
