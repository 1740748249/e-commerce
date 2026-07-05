package com.ecommerce.api.client.fallback;

import com.ecommerce.api.client.PaymentClient;
import com.ecommerce.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {
    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {
            @Override
            public R<Boolean> checkAndSyncPayment(Long orderNo) {
                log.error("Feign 调用 payment-service 查单同步失败: orderNo={}", orderNo, cause);
                return R.error("支付服务暂不可用");
            }
        };
    }
}
