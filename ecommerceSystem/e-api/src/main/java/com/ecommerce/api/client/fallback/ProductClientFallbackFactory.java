package com.ecommerce.api.client.fallback;

import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public R<List<ProductVO>> getDetailsByIds(Set<Long> ids) {
                log.error("Feign 调用 product-service 批量查询商品失败: ids={}", ids, cause);
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<ProductVO> getDetail(Long id) {
                log.error("Feign 调用 product-service 查询商品详情失败: id={}", id, cause);
                return R.error("商品服务暂不可用");
            }

            @Override
            public R<Long> getPendingShopCount() {
                log.error("Feign 调用 product-service 查询待审批店铺数失败", cause);
                return R.ok(0L);
            }

            @Override
            public R<Integer> getFlashOrderStatus(Long flashSaleOrderId) {
                log.error("Feign 调用 product-service 查询秒杀订单状态失败: flashSaleOrderId={}", flashSaleOrderId, cause);
                return R.error("商品服务暂不可用");
            }
        };
    }
}
