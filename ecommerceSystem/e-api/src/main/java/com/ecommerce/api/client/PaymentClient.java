package com.ecommerce.api.client;

import com.ecommerce.api.client.fallback.PaymentClientFallbackFactory;
import com.ecommerce.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "payment-service",
    fallbackFactory = PaymentClientFallbackFactory.class
)
public interface PaymentClient {

    @GetMapping("/payment/feign/check-and-sync/{orderNo}")
    R<Boolean> checkAndSyncPayment(@PathVariable("orderNo") Long orderNo);
}
