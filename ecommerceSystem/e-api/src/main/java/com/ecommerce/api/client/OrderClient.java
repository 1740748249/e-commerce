package com.ecommerce.api.client;

import com.ecommerce.api.client.fallback.OrderClientFallbackFactory;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "order-service",
    fallbackFactory = OrderClientFallbackFactory.class
)
public interface OrderClient {

    @GetMapping("/orders/feign/{orderNo}")
    R<OrderBasicDTO> getOrderBasic(@PathVariable("orderNo") Long orderNo);

    @PutMapping("/orders/{orderNo}/timeout-cancel")
    R<Void> cancelOrderByTimeout(@PathVariable("orderNo") Long orderNo);

    @PutMapping("/orders/{orderNo}/pay-callback")
    R<Void> payCallback(@PathVariable("orderNo") Long orderNo,
                        @RequestParam("payNo") String payNo,
                        @RequestParam("payTime") String payTime);

    @PutMapping("/orders/{orderNo}/refund-callback")
    R<Void> refundCallback(@PathVariable("orderNo") Long orderNo,
                           @RequestParam("refundAmount") Integer refundAmount);

    @GetMapping("/orders/statistics")
    R<OrderStatisticsDTO> getOrderStatistics();
}
