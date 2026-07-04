package com.ecommerce.order.controller;

import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.order.domain.dto.*;
import com.ecommerce.order.query.OrderPageQuery;
import com.ecommerce.order.domain.vo.OrderDetailVO;
import com.ecommerce.order.domain.vo.OrderPreviewVO;
import com.ecommerce.order.domain.vo.OrderVO;
import com.ecommerce.order.service.IEOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/orders")
@Slf4j
@RequiredArgsConstructor
public class EOrderController {

    private final IEOrderService orderService;

    @PostMapping
    public R<Long> create(@Valid @RequestBody CreateOrderDTO dto) {
        return orderService.create(dto);
    }

    @PostMapping("/buy-now")
    public R<Long> buyNow(@Valid @RequestBody BuyNowDTO dto) {
        return orderService.buyNow(dto);
    }

    @PostMapping("/preview")
    public R<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return orderService.preview(dto);
    }

    @GetMapping("/my")
    public R<PageDTO<OrderVO>> myOrders(OrderPageQuery query) {
        return orderService.myOrders(query);
    }

    @GetMapping("/shop")
    public R<PageDTO<OrderVO>> shopOrders(OrderPageQuery query) {
        return orderService.shopOrders(query);
    }

    @GetMapping("/{orderNo}")
    public R<OrderDetailVO> detail(@PathVariable Long orderNo) {
        return orderService.detail(orderNo);
    }

    @PutMapping("/{orderNo}/status")
    public R<Void> updateStatus(@PathVariable Long orderNo,
                                @Valid @RequestBody UpdateOrderStatusDTO dto) {
        return orderService.updateStatus(orderNo, dto.getStatus());
    }

    @PutMapping("/{orderNo}/cancel")
    public R<Void> cancel(@PathVariable Long orderNo) {
        return orderService.cancel(orderNo);
    }

    @PutMapping("/{orderNo}/timeout-cancel")
    public R<Void> cancelByTimeout(@PathVariable Long orderNo) {
        return orderService.cancelByTimeout(orderNo);
    }

    @PutMapping("/{orderNo}/pay-callback")
    public R<Void> payCallback(@PathVariable Long orderNo,
                                @RequestParam String payNo,
                                @RequestParam String payTime) {
        return orderService.payCallback(orderNo, payNo, payTime);
    }

    @PutMapping("/{orderNo}/refund-callback")
    public R<Void> refundCallback(@PathVariable Long orderNo,
                                  @RequestParam Integer refundAmount) {
        return orderService.refundCallback(orderNo, refundAmount);
    }

    @GetMapping("/statistics")
    public R<OrderStatisticsDTO> getStatistics() {
        return R.ok(orderService.getStatistics());
    }

    @GetMapping("/feign/{orderNo}")
    public R<OrderBasicDTO> getOrderBasic(@PathVariable Long orderNo) {
        OrderBasicDTO dto = orderService.getOrderBasic(orderNo);
        return dto != null ? R.ok(dto) : R.error("订单不存在");
    }
}
