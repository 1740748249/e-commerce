package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flash-sale/orders")
@RequiredArgsConstructor
public class EFlashSaleOrderController {

    private final IEFlashSaleOrderService flashSaleOrderService;

    @GetMapping("/{id}/status")
    public R<Integer> getStatus(@PathVariable Long id) {
        EFlashSaleOrder order = flashSaleOrderService.getById(id);
        if (order == null) return R.error("秒杀订单不存在");
        return R.ok(order.getStatus().getValue());
    }
}
