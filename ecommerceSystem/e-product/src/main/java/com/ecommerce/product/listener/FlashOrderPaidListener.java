package com.ecommerce.product.listener;

import com.ecommerce.api.message.FlashOrderPaidMessage;
import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.enums.FlashSaleOrderStatus;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashOrderPaidListener {

    private final IEFlashSaleOrderService flashSaleOrderService;

    @RabbitListener(queues = "flash.order.paid.queue")
    public void onFlashOrderPaid(FlashOrderPaidMessage msg) {
        EFlashSaleOrder order = flashSaleOrderService.getById(msg.getFlashSaleOrderId());
        if (order == null) {
            log.warn("秒杀订单不存在: flashSaleOrderId={}", msg.getFlashSaleOrderId());
            return;
        }

        if (order.getStatus() == FlashSaleOrderStatus.PAID) {
            return;
        }

        if (order.getStatus() != FlashSaleOrderStatus.PENDING_PAYMENT) {
            log.warn("秒杀订单状态异常，收到支付消息但当前状态为 {}: flashSaleOrderId={}",
                    order.getStatus(), msg.getFlashSaleOrderId());
            return;
        }

        boolean updated = flashSaleOrderService.lambdaUpdate()
                .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAID)
                .eq(EFlashSaleOrder::getId, order.getId())
                .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                .update();

        if (updated) {
            log.info("秒杀订单已标记支付成功: flashSaleOrderId={}", msg.getFlashSaleOrderId());
        } else {
            log.info("秒杀订单状态已被其他操作变更: flashSaleOrderId={}", msg.getFlashSaleOrderId());
        }
    }
}
