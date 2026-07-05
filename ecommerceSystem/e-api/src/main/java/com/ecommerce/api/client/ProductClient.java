package com.ecommerce.api.client;

import com.ecommerce.api.client.fallback.ProductClientFallbackFactory;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

@FeignClient(
    name = "product-service",
    fallbackFactory = ProductClientFallbackFactory.class
)
public interface ProductClient {

    /**
     * 根据商品ID批量查询商品详情（含SKU）
     */
    @GetMapping("/products/list")
    R<List<ProductVO>> getDetailsByIds(@RequestParam("ids") Set<Long> ids);

    /**
     * 查询单个商品详情
     */
    @GetMapping("/products/{id}")
    R<ProductVO> getDetail(@PathVariable("id") Long id);

    @GetMapping("/admin/shops/pending/count")
    R<Long> getPendingShopCount();

    @GetMapping("/flash-sale/orders/{id}/status")
    R<Integer> getFlashOrderStatus(@PathVariable("id") Long flashSaleOrderId);

    /**
     * 根据店主 userId 查询已审批通过的店铺 ID，用于通知服务 WebSocket 鉴权的缓存回源。
     */
    @GetMapping("/shops/owner/{ownerId}")
    R<Long> getShopIdByOwner(@PathVariable("ownerId") Long ownerId);

    /**
     * 获取所有已审批通过的店铺 ID，用于通知服务广播时递增全部未读计数。
     */
    @GetMapping("/shops/ids")
    R<List<Long>> getAllApprovedShopIds();
}
