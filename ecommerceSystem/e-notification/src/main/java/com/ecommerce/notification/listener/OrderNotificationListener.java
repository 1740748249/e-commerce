package com.ecommerce.notification.listener;

import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.notification.service.IENotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.notification.constants.MqConstants.ORDER_NOTIFY_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final IENotificationService notificationService;

    @RabbitListener(queues = ORDER_NOTIFY_QUEUE)
    public void onOrderNotification(OrderNotificationMessage msg) {
        log.info("收到订单通知: orderNo={}, type={}", msg.getOrderNo(), msg.getType());
        notificationService.createNotification(msg);
    }
}
