package com.ecommerce.order.listener;

import com.ecommerce.api.message.FlashSaleOrderMessage;
import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.order.domain.po.EOrder;
import com.ecommerce.order.domain.po.EOrderItem;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.mapper.EOrderItemMapper;
import com.ecommerce.order.mapper.EOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static com.ecommerce.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_NOTIFY_KEY;
import static com.ecommerce.order.constants.MqConstants.FLASH_ORDER_CREATE_QUEUE;

/**
 * 秒杀订单异步创建消费者。
 * <p>
 * 消费 product-service 发送的秒杀订单消息，幂等地创建正式订单 + 订单明细。
 * 消息投递语义为 at-least-once，通过 Redis setIfAbsent 保证幂等。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleOrderListener {

    private final EOrderMapper orderMapper;
    private final EOrderItemMapper orderItemMapper;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    /** 幂等 key 前缀，格式：order:flash:{flashSaleOrderId}，TTL 24 小时 */
    private static final String IDEMPOTENT_PREFIX = "order:flash:";

    /**
     * 处理秒杀订单创建消息。
     *
     * @param msg 秒杀订单消息体（product-service → order-service）
     */
    @Transactional
    @RabbitListener(queues = FLASH_ORDER_CREATE_QUEUE)
    public void onCreateOrder(FlashSaleOrderMessage msg) {
        log.info("处理秒杀订单创建: flashSaleOrderId={}, userId={}", msg.getFlashSaleOrderId(), msg.getUserId());

        String idempotentKey = IDEMPOTENT_PREFIX + msg.getFlashSaleOrderId();

        // 1. 幂等校验：setIfAbsent 必须在事务内，失败时删除 key 以允许 MQ 重试
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(first)) {
            log.info("重复消息，跳过: flashSaleOrderId={}", msg.getFlashSaleOrderId());
            return;
        }

        // 2. 设置用户上下文（后续 Feign 调用等依赖 ThreadLocal 传递 userId/shopId）
        UserContext.set(new UserContext.UserInfo(msg.getUserId(), msg.getShopId(), null));
        try {
            // 3. 创建正式订单（使用 product-service 预生成的订单号）
            EOrder order = BeanUtils.copyBean(msg, EOrder.class, (src, target) -> {
                target.setOrderNo(msg.getOrderNo());
                target.setFlashSaleOrderId(msg.getFlashSaleOrderId());
                target.setUserId(msg.getUserId());
                target.setShopId(msg.getShopId());
                target.setAddressId(msg.getAddressId());
                target.setReceiverName(msg.getReceiverName());
                target.setReceiverPhone(msg.getReceiverPhone());
                target.setReceiverAddr(msg.getReceiverAddr());
                target.setTotalAmount(src.getPrice() * src.getQuantity());
                target.setDiscountAmount(0);
                target.setStatus(OrderStatus.PENDING_PAYMENT);
            });
            orderMapper.insert(order);

            // 4. 创建订单明细
            EOrderItem item = BeanUtils.copyBean(msg, EOrderItem.class, (src, target) -> {
                target.setOrderId(order.getId());
            });
            orderItemMapper.insert(item);

            // 5. 发送通知 MQ（事务提交后执行）
            final long notifyOrderNo = order.getOrderNo();
            final long notifyShopId = msg.getShopId();
            final int total = msg.getPrice() * msg.getQuantity();
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            OrderNotificationMessage notifyMsg = OrderNotificationMessage.builder()
                                    .orderNo(notifyOrderNo)
                                    .shopId(notifyShopId)
                                    .type(NotificationType.NEW_ORDER)
                                    .title("新订单通知")
                                    .content("秒杀订单，金额 ¥" + String.format("%.2f", total / 100.0))
                                    .build();
                            rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, notifyMsg);
                        }
                    });

            log.info("秒杀订单创建完成: orderNo={}, flashSaleOrderId={}", order.getOrderNo(), msg.getFlashSaleOrderId());
        } catch (Exception e) {
            // 事务回滚时删除幂等标记，否则 MQ 重试会被跳过导致订单永久丢失
            try {
                redisTemplate.delete(idempotentKey);
            } catch (Exception re) {
                log.error("删除幂等标记失败，订单可能丢失 idempotentKey={}", idempotentKey, re);
                // 同时保留 DB 异常和 Redis 异常信息
                RuntimeException combined = new RuntimeException(
                        "DB操作失败且幂等标记清理失败: " + e.getMessage(), re);
                combined.addSuppressed(e);
                throw combined;
            }
            throw e;
        } finally {
            // 5. 清理 ThreadLocal，防止 MQ 消费线程复用时上下文泄漏
            UserContext.remove();
        }
    }
}
