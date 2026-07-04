package com.ecommerce.product.job;

import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.service.IEFlashSaleService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecommerce.product.constants.CacheConstants.FLASH_STOCK_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashStockReconciliationJob {

    private final IEFlashSaleService flashSaleService;
    private final StringRedisTemplate redisTemplate;

    @XxlJob("flashStockReconciliation")
    public void reconcile() {
        log.info("秒杀库存对账任务开始");

        List<EFlashSale> sales = flashSaleService.lambdaQuery().list();
        if (sales.isEmpty()) {
            log.info("秒杀库存对账任务结束，无秒杀活动");
            return;
        }

        int synced = 0;
        int skipped = 0;
        for (EFlashSale sale : sales) {
            try {
                String redisStock = redisTemplate.opsForValue()
                        .get(FLASH_STOCK_PREFIX + sale.getId());
                if (redisStock == null) {
                    skipped++;
                    continue;
                }
                int stock = Integer.parseInt(redisStock);
                if (sale.getStock() == null || stock != sale.getStock()) {
                    flashSaleService.lambdaUpdate()
                            .set(EFlashSale::getStock, stock)
                            .eq(EFlashSale::getId, sale.getId())
                            .update();
                    synced++;
                }
            } catch (Exception e) {
                log.error("对账失败 flashSaleId={}", sale.getId(), e);
            }
        }

        log.info("秒杀库存对账任务结束: 总计={}, 已同步={}, 已跳过(Redis无值)={}",
                sales.size(), synced, skipped);
    }
}
