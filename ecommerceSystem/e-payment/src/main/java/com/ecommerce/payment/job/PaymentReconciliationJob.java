package com.ecommerce.payment.job;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.payment.domain.po.EPaymentRecord;
import com.ecommerce.payment.domain.po.ERefundRecord;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.enums.RefundStatus;
import com.ecommerce.payment.service.IEPaymentRecordService;
import com.ecommerce.payment.service.IERefundRecordService;
import com.ecommerce.payment.service.IRefundService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final IEPaymentRecordService paymentRecordService;
    private final IERefundRecordService refundRecordService;
    private final IRefundService refundService;
    private final AlipayClient alipayClient;
    private final OrderClient orderClient;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @XxlJob("paymentReconciliation")
    public void reconcile() {
        log.info("支付对账任务开始");
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);

        List<EPaymentRecord> pendingRecords = paymentRecordService.lambdaQuery()
                .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)
                .le(EPaymentRecord::getCreateTime, deadline)
                .last("LIMIT 500")
                .list();

        if (pendingRecords.isEmpty()) {
            log.info("支付对账任务结束，无待对账记录");
            return;
        }

        int successCount = 0;
        for (EPaymentRecord record : pendingRecords) {
            try {
                if (checkAndSync(record)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("对账异常: orderNo={}", record.getOrderNo(), e);
            }
        }
        log.info("支付对账任务结束，处理 {} 笔，成功 {} 笔", pendingRecords.size(), successCount);
    }

    private boolean checkAndSync(EPaymentRecord record) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(record.getOrderNo().toString());
        request.setBizModel(model);

        AlipayTradeQueryResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("支付宝查单失败: orderNo={}", record.getOrderNo(), e);
            return false;
        }

        if (!response.isSuccess()) {
            // ACQ.TRADE_NOT_EXIST 为终态错误，交易在支付宝侧不存在，标记已关闭停止重试
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                paymentRecordService.lambdaUpdate()
                        .set(EPaymentRecord::getStatus, PaymentStatus.CLOSED)
                        .eq(EPaymentRecord::getId, record.getId())
                        .update();
                log.info("支付宝交易不存在，记录已关闭: orderNo={}", record.getOrderNo());
            }
            return false;
        }

        String tradeStatus = response.getTradeStatus();
        // 非支付成功的状态（TRADE_CLOSED / WAIT_BUYER_PAY 等）一律跳过，
        // 让订单超时 MQ 延时任务处理订单取消，对账任务只负责"支付成功但漏同步"的兜底
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return false;
        }

        String totalAmountStr = response.getTotalAmount();
        if (totalAmountStr == null) {
            log.error("支付宝查单缺少金额: orderNo={}", record.getOrderNo());
            return false;
        }
        int alipayAmountFen = new BigDecimal(totalAmountStr).movePointRight(2).intValue();
        if (!record.getTotalAmount().equals(alipayAmountFen)) {
            log.error("对账金额不匹配: orderNo={}, local={}, alipay={}",
                    record.getOrderNo(), record.getTotalAmount(), alipayAmountFen);
            return false;
        }

        String payTimeStr;
        LocalDateTime payTime;
        if (response.getSendPayDate() != null) {
            payTime = response.getSendPayDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            payTimeStr = payTime.format(FMT);
        } else {
            payTime = LocalDateTime.now();
            payTimeStr = payTime.format(FMT);
        }

        // 先回调 order-service（幂等），再写本地记录
        // 回调网络异常 → 不更新 DB，下次对账重试
        try {
            R<Void> callbackR = orderClient.payCallback(record.getOrderNo(), response.getTradeNo(), payTimeStr);
            if (!callbackR.success()) {
                log.warn("对账回调返回非成功: orderNo={}, msg={}",
                        record.getOrderNo(), callbackR.getMessage());
                // 回调失败 → 查订单状态，若订单已终态（取消/退款）则关闭支付记录停止重试
                if (isOrderTerminal(record.getOrderNo())) {
                    paymentRecordService.lambdaUpdate()
                            .set(EPaymentRecord::getStatus, PaymentStatus.CLOSED)
                            .eq(EPaymentRecord::getId, record.getId())
                            .update();
                    log.warn("订单已终态，支付记录已关闭: orderNo={}", record.getOrderNo());
                }
                return false;
            }
        } catch (Exception e) {
            log.error("对账回调 order-service 异常，跳过本次更新等待下次重试: orderNo={}", record.getOrderNo(), e);
            return false;
        }

        boolean updated = paymentRecordService.lambdaUpdate()
                .set(EPaymentRecord::getPayNo, response.getTradeNo())
                .set(EPaymentRecord::getStatus, PaymentStatus.SUCCESS)
                .set(EPaymentRecord::getPayTime, payTime)
                .eq(EPaymentRecord::getId, record.getId())
                .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)
                .update();

        if (!updated) {
            log.info("支付记录已被其他线程更新: orderNo={}", record.getOrderNo());
        }

        log.info("对账成功: orderNo={}, payNo={}", record.getOrderNo(), response.getTradeNo());
        return true;
    }

    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int ORDER_STATUS_REFUNDED = 6;

    private boolean isOrderTerminal(Long orderNo) {
        try {
            R<OrderBasicDTO> r = orderClient.getOrderBasic(orderNo);
            if (!r.success() || r.getData() == null) return false;
            Integer status = r.getData().getStatus();
            return status != null && (status == ORDER_STATUS_CANCELLED || status == ORDER_STATUS_REFUNDED);
        } catch (Exception e) {
            log.error("查询订单状态异常: orderNo={}", orderNo, e);
            return false;
        }
    }

    // 退款对账：仅扫描 PROCESSING 状态记录（不确定是否已退款），调支付宝查询结果
    @XxlJob("refundReconciliation")
    public void reconcileRefunds() {
        log.info("退款对账任务开始");
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5);

        // 只查 PROCESSING 记录——这些是退款发起后网络异常、未确定结果的记录，数量极少
        List<ERefundRecord> processingRecords = refundRecordService.lambdaQuery()
                .eq(ERefundRecord::getStatus, RefundStatus.PROCESSING)
                .le(ERefundRecord::getCreateTime, deadline)
                .last("LIMIT 200")
                .list();

        if (processingRecords.isEmpty()) {
            log.info("退款对账任务结束，无待对账记录");
            return;
        }

        int resolved = 0;
        for (ERefundRecord record : processingRecords) {
            try {
                refundService.queryRefund(record.getOutRequestNo());
                // queryRefund 内部已更新 DB 状态，此处只计数
                resolved++;
            } catch (Exception e) {
                log.error("退款对账异常: outRequestNo={}", record.getOutRequestNo(), e);
            }
        }
        log.info("退款对账任务结束，处理 {} 笔，已解决 {} 笔", processingRecords.size(), resolved);
    }
}
