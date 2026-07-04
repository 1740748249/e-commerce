package com.ecommerce.product.listener;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.dto.StockSyncBatchMessage;
import com.ecommerce.api.dto.StockSyncBatchMessage.SkuItem;
import com.ecommerce.product.domain.po.EProductSku;
import com.ecommerce.product.mapper.EProductMapper;
import com.ecommerce.product.mapper.EProductSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecommerce.product.constants.CacheConstants.MQ_DEDUP_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncListener extends ServiceImpl<EProductSkuMapper, EProductSku> {

    private final StringRedisTemplate redisTemplate;
    private final EProductMapper eProductMapper;

    @RabbitListener(queues = "stock.sync.queue")
    @Transactional
    public void onStockSync(StockSyncBatchMessage msg) {
        if (!dedupCheck(msg.getMessageId())) return;
        List<SkuItem> merged = mergeSkuItems(msg.getItems());
        try {
            int affected = baseMapper.batchDeductStock(merged);
            log.debug("批量库存扣减完成: expected={}, affected={}", merged.size(), affected);
            if (affected < merged.size()) {
                log.warn("库存扣减部分行未命中: expected={}, affected={}", merged.size(), affected);
            }
            syncSpuTotalStock(merged, false);
        } catch (Exception e) {
            removeDedupKey(msg.getMessageId());
            throw e;
        }
    }

    @RabbitListener(queues = "stock.restore.queue")
    @Transactional
    public void onStockRestore(StockSyncBatchMessage msg) {
        if (!dedupCheck(msg.getMessageId())) return;
        List<SkuItem> merged = mergeSkuItems(msg.getItems());
        try {
            baseMapper.batchRestoreStock(merged);
            log.debug("批量库存恢复完成: count={}", merged.size());
            syncSpuTotalStock(merged, true);
        } catch (Exception e) {
            removeDedupKey(msg.getMessageId());
            throw e;
        }
    }

    /**
     * 同步更新 SPU 的 total_stock。
     * 先查 SKU 获取 productId，再按 product 聚合数量，批量更新 e_product。
     */
    private void syncSpuTotalStock(List<SkuItem> skuItems, boolean restore) {
        List<Long> skuIds = skuItems.stream().map(SkuItem::getSkuId).collect(Collectors.toList());
        List<EProductSku> skus = lambdaQuery().in(EProductSku::getId, skuIds).list();

        Map<Long, Long> skuToProduct = skus.stream()
                .collect(Collectors.toMap(EProductSku::getId, EProductSku::getProductId));
        Map<Long, Integer> productQty = new HashMap<>();
        for (SkuItem item : skuItems) {
            Long productId = skuToProduct.get(item.getSkuId());
            if (productId != null) {
                productQty.merge(productId, item.getQuantity(), Integer::sum);
            }
        }

        if (productQty.isEmpty()) return;

        List<SkuItem> productItems = productQty.entrySet().stream()
                .map(e -> new SkuItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        if (restore) {
            eProductMapper.batchRestoreTotalStock(productItems);
        } else {
            eProductMapper.batchDeductTotalStock(productItems);
        }
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

    private void removeDedupKey(String messageId) {
        try {
            redisTemplate.delete(MQ_DEDUP_PREFIX + messageId);
        } catch (Exception ignored) {
        }
    }

    private List<SkuItem> mergeSkuItems(List<SkuItem> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        SkuItem::getSkuId,
                        SkuItem::getQuantity,
                        Integer::sum))
                .entrySet().stream()
                .map(e -> new SkuItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
