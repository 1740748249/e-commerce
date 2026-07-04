package com.ecommerce.product.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.product.domain.po.EProductSku;
import com.ecommerce.product.mapper.EProductSkuMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecommerce.product.constants.CacheConstants.SKU_STOCK_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkuStockReconciliationJob {

    private final EProductSkuMapper skuMapper;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("skuStockReconciliation")
    public void reconcile() {
        log.info("SKU 库存对账任务开始");

        List<EProductSku> skus = skuMapper.selectList(null);
        if (skus.isEmpty()) {
            log.info("SKU 库存对账任务结束，无 SKU 记录");
            return;
        }

        int synced = 0;
        int skipped = 0;
        for (EProductSku sku : skus) {
            try {
                String redisStock = redisTemplate.opsForValue()
                        .get(SKU_STOCK_PREFIX + sku.getId());
                if (redisStock == null) {
                    skipped++;
                    continue;
                }
                int stock = Integer.parseInt(redisStock);
                if (sku.getStock() == null || stock != sku.getStock()) {
                    skuMapper.update(null,
                            new LambdaUpdateWrapper<EProductSku>()
                                    .set(EProductSku::getStock, stock)
                                    .eq(EProductSku::getId, sku.getId()));
                    synced++;
                }
            } catch (Exception e) {
                log.error("SKU 库存对账失败 skuId={}", sku.getId(), e);
            }
        }

        log.info("SKU 库存对账任务结束: 总计={}, 已同步={}, 跳过(Redis无值)={}",
                skus.size(), synced, skipped);
    }
}
