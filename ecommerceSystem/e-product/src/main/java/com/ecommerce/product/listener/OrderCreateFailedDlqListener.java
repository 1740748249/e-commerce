package com.ecommerce.product.listener;

import com.ecommerce.api.message.OrderCreateFailedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.ecommerce.product.constants.CacheConstants.SKU_STOCK_PREFIX;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateFailedDlqListener {

    private final StringRedisTemplate redisTemplate;

    private static final String DEDUP_PREFIX = "dlq:order_create_failed:";

    @RabbitListener(queues = "order.create.failed.dlq")
    public void onOrderCreateFailed(OrderCreateFailedMessage msg) {
        if (!dedupCheck(msg.getMessageId())) {
            return;
        }

        log.warn("DLQ 兜底回滚 Redis SKU 库存: messageId={}, items={}",
                msg.getMessageId(), msg.getItems().size());
        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings({"rawtypes", "unchecked"})
                public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                    for (OrderCreateFailedMessage.SkuItem item : msg.getItems()) {
                        ops.opsForValue().increment(
                                SKU_STOCK_PREFIX + item.getSkuId(), item.getQuantity());
                    }
                    return null;
                }
            });
            log.info("DLQ 兜底回滚成功: messageId={}", msg.getMessageId());
        } catch (Exception e) {
            redisTemplate.delete(DEDUP_PREFIX + msg.getMessageId());
            log.error("DLQ 兜底回滚失败，等待 MQ 重试: messageId={}", msg.getMessageId(), e);
            throw e;
        }
    }

    private boolean dedupCheck(String messageId) {
        if (messageId == null) {
            return true;
        }
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(DEDUP_PREFIX + messageId, "1", Duration.ofHours(24));
        if (absent == null || !absent) {
            log.info("重复 DLQ 消息，跳过: messageId={}", messageId);
            return false;
        }
        return true;
    }
}
