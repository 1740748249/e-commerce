package com.ecommerce.order.listener;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.dto.StockSyncBatchMessage;
import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.order.domain.dto.OrderTimeoutMessage;
import com.ecommerce.order.domain.po.EOrder;
import com.ecommerce.order.domain.po.EUserCoupon;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.UserCouponStatus;
import com.ecommerce.order.mapper.EOrderMapper;
import com.ecommerce.order.service.IEUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_NOTIFY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_RESTORE_KEY;
import static com.ecommerce.order.constants.MqConstants.ORDER_DELAY_QUEUE;
import static com.ecommerce.order.constants.RedisConstants.SKU_STOCK_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener extends ServiceImpl<EOrderMapper, EOrder> {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;
    private final IEUserCouponService userCouponService;

    @RabbitListener(queues = ORDER_DELAY_QUEUE)
    @Transactional
    public void onOrderTimeout(OrderTimeoutMessage msg) {
        log.info("订单超时检查: orderNo={}", msg.getOrderNo());

        // 1. 乐观锁取消订单（仅待支付）
        boolean cancelled = lambdaUpdate()
                .eq(EOrder::getOrderNo, msg.getOrderNo())
                .eq(EOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(EOrder::getStatus, OrderStatus.CANCELLED)
                .update();
        if (!cancelled) {
            log.info("订单已支付或已取消，跳过: orderNo={}", msg.getOrderNo());
            return;
        }

        // 2. 退回优惠券
        userCouponService.lambdaUpdate()
                .eq(EUserCoupon::getOrderNo, msg.getOrderNo())
                .eq(EUserCoupon::getStatus, UserCouponStatus.USED)
                .set(EUserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(EUserCoupon::getUsedAt, null)
                .set(EUserCoupon::getOrderNo, null)
                .update();

        // 3. Redis 恢复 + MQ 放 afterCommit，避免重试时重复处理
        if (msg.getItems() != null) {
            List<StockSyncBatchMessage.SkuItem> mergedItems = mergeSkuItems(
                    msg.getItems().stream()
                            .map(i -> new StockSyncBatchMessage.SkuItem(i.getSkuId(), i.getQuantity()))
                            .collect(Collectors.toList()));

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisTemplate.executePipelined(new SessionCallback<Object>() {
                                @Override
                                @SuppressWarnings({"rawtypes", "unchecked"})
                                public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                                    for (StockSyncBatchMessage.SkuItem item : mergedItems) {
                                        ops.opsForValue().increment(SKU_STOCK_PREFIX + item.getSkuId(), item.getQuantity());
                                    }
                                    return null;
                                }
                            });
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, STOCK_RESTORE_KEY,
                                    new StockSyncBatchMessage(IdUtil.fastSimpleUUID(), mergedItems));
                            OrderNotificationMessage notifyMsg = OrderNotificationMessage.builder()
                                    .orderNo(msg.getOrderNo())
                                    .shopId(msg.getShopId())
                                    .type(NotificationType.NEW_ORDER)
                                    .title("订单已取消")
                                    .content("订单 #" + msg.getOrderNo() + " 已超时自动取消，金额 ¥"
                                            + String.format("%.2f", (msg.getTotalAmount() != null ? msg.getTotalAmount() : 0) / 100.0))
                                    .build();
                            rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, notifyMsg);
                        }
                    });
        }

        log.info("订单已超时取消: orderNo={}", msg.getOrderNo());
    }

    private List<StockSyncBatchMessage.SkuItem> mergeSkuItems(List<StockSyncBatchMessage.SkuItem> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        StockSyncBatchMessage.SkuItem::getSkuId,
                        StockSyncBatchMessage.SkuItem::getQuantity,
                        Integer::sum))
                .entrySet().stream()
                .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
