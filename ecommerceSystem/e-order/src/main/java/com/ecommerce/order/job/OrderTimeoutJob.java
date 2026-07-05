package com.ecommerce.order.job;

import com.ecommerce.api.client.PaymentClient;
import com.ecommerce.common.domain.R;
import com.ecommerce.order.domain.po.EOrder;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.service.IEOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final IEOrderService orderService;
    private final PaymentClient paymentClient;

    @XxlJob("orderTimeoutCancel")
    public void cancelTimeout() {
        log.info("普通订单超时取消任务开始");
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);

        List<EOrder> timeoutOrders = orderService.lambdaQuery()
                .eq(EOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .le(EOrder::getCreateTime, deadline)
                .last("LIMIT 500")
                .list();

        if (timeoutOrders.isEmpty()) {
            log.info("普通订单超时取消任务结束，无超时订单");
            return;
        }

        int success = 0;
        int recovered = 0;
        int skip = 0;
        for (EOrder order : timeoutOrders) {
            try {
                // 取消前先查支付宝真实状态，防止因回调丢失误取消已付款订单
                if (tryRecoverPaidOrder(order.getOrderNo())) {
                    recovered++;
                    continue;
                }
                orderService.cancelByTimeout(order.getOrderNo());
                success++;
            } catch (Exception e) {
                log.error("超时取消订单失败: orderNo={}", order.getOrderNo(), e);
                skip++;
            }
        }
        log.info("普通订单超时取消任务结束，取消 {} 笔，恢复已支付 {} 笔，跳过 {} 笔", success, recovered, skip);
    }

    private boolean tryRecoverPaidOrder(Long orderNo) {
        try {
            R<Boolean> r = paymentClient.checkAndSyncPayment(orderNo);
            if (r.success() && Boolean.TRUE.equals(r.getData())) {
                log.info("超时取消前恢复已支付订单: orderNo={}", orderNo);
                return true;
            }
        } catch (Exception e) {
            log.error("调用支付服务查单异常，将正常取消订单: orderNo={}", orderNo, e);
        }
        return false;
    }
}
