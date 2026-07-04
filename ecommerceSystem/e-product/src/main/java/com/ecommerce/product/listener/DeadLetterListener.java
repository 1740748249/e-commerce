package com.ecommerce.product.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterListener {

    @RabbitListener(queues = "dead.letter.queue")
    public void onDeadLetter(Message message) {
        log.error("========== 收到死信消息 ==========");
        log.error("  Exchange: {}", message.getMessageProperties().getReceivedExchange());
        log.error("  RoutingKey: {}", message.getMessageProperties().getReceivedRoutingKey());
        log.error("  Body: {}", new String(message.getBody()));
        log.error("  Headers: {}", message.getMessageProperties().getHeaders());
        log.error("===================================");
    }
}
