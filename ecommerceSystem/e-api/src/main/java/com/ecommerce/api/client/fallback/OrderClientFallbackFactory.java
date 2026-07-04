package com.ecommerce.api.client.fallback;

import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    @Override
    public OrderClient create(Throwable cause) {
        return new OrderClient() {
            @Override
            public R<OrderBasicDTO> getOrderBasic(Long orderNo) {
                log.error("Feign 调用 order-service 查询订单失败: orderNo={}", orderNo, cause);
                return R.error("订单服务暂不可用");
            }

            @Override
            public R<Void> cancelOrderByTimeout(Long orderNo) {
                log.error("Feign 调用 order-service 超时取消订单失败: orderNo={}", orderNo, cause);
                return R.error("订单服务暂不可用");
            }

            @Override
            public R<Void> payCallback(Long orderNo, String payNo, String payTime) {
                log.error("Feign 调用 order-service 支付回调失败: orderNo={}", orderNo, cause);
                return R.error("订单服务暂不可用");
            }

            @Override
            public R<Void> refundCallback(Long orderNo, Integer refundAmount) {
                log.error("Feign 调用 order-service 退款回调失败: orderNo={}", orderNo, cause);
                return R.error("订单服务暂不可用");
            }

            @Override
            public R<OrderStatisticsDTO> getOrderStatistics() {
                log.error("Feign 调用 order-service 查询订单统计失败", cause);
                return R.ok(new OrderStatisticsDTO());
            }
        };
    }
}
