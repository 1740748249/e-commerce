package com.ecommerce.order.listener;

import com.ecommerce.order.domain.dto.CartSyncMessage;
import com.ecommerce.order.service.impl.ECartServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.ecommerce.order.constants.MqConstants.CART_SYNC_QUEUE;
import static com.ecommerce.order.constants.RedisConstants.CART_SYNC_VER_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartSyncListener {

    private final StringRedisTemplate redisTemplate;
    private final ECartServiceImpl cartService;

    @RabbitListener(queues = CART_SYNC_QUEUE)
    public void onCartSyncMessage(CartSyncMessage msg) {
        String verKey = CART_SYNC_VER_PREFIX + msg.getUserId() + ":" + msg.getCartItemId();
        String currentVer = redisTemplate.opsForValue().get(verKey);
        if (currentVer != null && Long.parseLong(currentVer) != msg.getVersion()) {
            log.debug("购物车落库消息版本号不匹配，丢弃: userId={}, cartItemId={}, msgVer={}, currentVer={}",
                    msg.getUserId(), msg.getCartItemId(), msg.getVersion(), currentVer);
            return;
        }
        cartService.syncToDb(msg.getUserId(), msg.getCartItemId());
        redisTemplate.delete(verKey);
    }
}
