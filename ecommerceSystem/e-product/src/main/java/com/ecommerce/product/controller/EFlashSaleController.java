package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.vo.FlashSaleOrderVO;
import com.ecommerce.product.domain.vo.FlashSaleVO;
import com.ecommerce.product.service.IEFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/flash-sales")
@RequiredArgsConstructor
@Validated
public class EFlashSaleController {
    private final IEFlashSaleService flashSaleService;

    @GetMapping
    public R<List<FlashSaleVO>> list(@RequestParam(required = false) Long sessionId) {
        return flashSaleService.listFlashSales(sessionId);
    }

    @PostMapping("/{id}/order")
    public R<FlashSaleOrderVO> order(@PathVariable @NotNull Long id,
                                     @RequestParam(defaultValue = "1") @Min(1) Integer quantity,
                                     @RequestParam @NotNull Long addressId) {
        return flashSaleService.order(id, quantity, addressId);
    }

    @GetMapping("/{id}/result")
    public R<FlashSaleOrderVO> result(@PathVariable @NotNull Long id) {
        return flashSaleService.result(id);
    }
}
