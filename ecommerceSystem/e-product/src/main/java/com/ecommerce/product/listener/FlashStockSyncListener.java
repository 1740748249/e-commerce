package com.ecommerce.product.listener;

import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.service.IEFlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static com.ecommerce.product.constants.CacheConstants.MQ_DEDUP_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashStockSyncListener {

    private final IEFlashSaleService flashSaleService;
    private final StringRedisTemplate redisTemplate;

    // 扣减 DB 库存，WHERE stock >= quantity 防超卖
    @RabbitListener(queues = "flash.stock.sync.queue")
    public void onSync(FlashStockSyncMessage msg) {
        if (!dedupCheck(msg.getMessageId())) return;
        boolean updated = flashSaleService.lambdaUpdate()
                .setSql("stock = stock - " + msg.getQuantity())
                .eq(EFlashSale::getId, msg.getFlashSaleId())
                .ge(EFlashSale::getStock, msg.getQuantity())
                .update();
        if (!updated) {
            log.warn("秒杀库存扣减落库失败（库存不足或活动不存在）: flashSaleId={}, quantity={}",
                    msg.getFlashSaleId(), msg.getQuantity());
        }
    }

    // 回补 DB 库存（取消 / 超时 / DLQ 补偿）
    @RabbitListener(queues = "flash.stock.restore.queue")
    public void onRestore(FlashStockSyncMessage msg) {
        if (!dedupCheck(msg.getMessageId())) return;
        flashSaleService.lambdaUpdate()
                .setSql("stock = stock + " + msg.getQuantity())
                .eq(EFlashSale::getId, msg.getFlashSaleId())
                .update();
        log.info("秒杀库存回补落库: flashSaleId={}, quantity={}", msg.getFlashSaleId(), msg.getQuantity());
    }

    private boolean dedupCheck(String messageId) {
        if (messageId == null) return true;
        Boolean absent = redisTemplate.opsForValue()
                .setIfAbsent(MQ_DEDUP_PREFIX + messageId, "1", Duration.ofHours(24));
        if (absent == null || !absent) {
            log.info("重复消息，跳过: messageId={}", messageId);
            return false;
        }
        return true;
    }
}
