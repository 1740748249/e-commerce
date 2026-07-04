package com.ecommerce.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.payment.config.AlipayProperties;
import com.ecommerce.payment.domain.dto.AlipayNotifyDTO;
import com.ecommerce.payment.domain.po.EPaymentRecord;
import com.ecommerce.payment.domain.po.ERefundRecord;
import com.ecommerce.payment.domain.vo.RefundResultVO;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.enums.RefundStatus;
import com.ecommerce.payment.mapper.ERefundRecordMapper;
import com.ecommerce.payment.service.IEPaymentRecordService;
import com.ecommerce.payment.service.IRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl extends ServiceImpl<ERefundRecordMapper, ERefundRecord> implements IRefundService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final OrderClient orderClient;
    private final IEPaymentRecordService paymentRecordService;

    @Override
    @Transactional
    public RefundResultVO refund(Long orderNo, Integer refundAmount, String reason) {
        Long userId = UserContext.getUserId();
        R<OrderBasicDTO> r = orderClient.getOrderBasic(orderNo);
        if (r == null || !r.success() || r.getData() == null) {
            throw new BadRequestException("订单不存在");
        }
        if (userId != null && !r.getData().getUserId().equals(userId)) {
            throw new BadRequestException("无权操作该订单");
        }

        EPaymentRecord payRecord = paymentRecordService.lambdaQuery()
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .last("LIMIT 1")
                .one();
        if (payRecord == null || payRecord.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("未找到成功支付记录");
        }
        if (refundAmount > payRecord.getTotalAmount()) {
            throw new BadRequestException("退款金额超过支付金额");
        }

        ERefundRecord existRefund = lambdaQuery()
                .eq(ERefundRecord::getOrderNo, orderNo)
                .last("LIMIT 1")
                .one();
        if (existRefund != null) {
            if (existRefund.getStatus() == RefundStatus.SUCCESS) {
                // 补偿回调：防止上次回调失败导致订单状态未更新（幂等安全）
                callbackOrder(orderNo, existRefund.getRefundAmount());
                return buildRefundResultVO(existRefund);
            }
            // PROCESSING 或 FAILED：先查询支付宝确认实际状态
            queryAlipayRefundAndSync(existRefund);
            existRefund = lambdaQuery().eq(ERefundRecord::getId, existRefund.getId()).one();
            if (existRefund != null && existRefund.getStatus() == RefundStatus.SUCCESS) {
                return buildRefundResultVO(existRefund);
            }
            if (existRefund != null && existRefund.getStatus() == RefundStatus.PROCESSING) {
                throw new BadRequestException("该订单已有退款处理中，请稍后查询退款状态");
            }
            // FAILED：允许重新发起
        }

        String outRequestNo = System.currentTimeMillis() + RandomUtil.randomNumbers(4);

        // 先存 PROCESSING 记录，防网络异常时退款意图丢失
        ERefundRecord refundRecord = new ERefundRecord();
        refundRecord.setOutRequestNo(outRequestNo);
        refundRecord.setPayNo(payRecord.getPayNo());
        refundRecord.setOrderNo(orderNo);
        refundRecord.setRefundAmount(refundAmount);
        refundRecord.setReason(reason);
        refundRecord.setStatus(RefundStatus.PROCESSING);
        save(refundRecord);

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setTradeNo(payRecord.getPayNo());
        model.setRefundAmount(BigDecimal.valueOf(refundAmount, 2).toPlainString());
        model.setOutRequestNo(outRequestNo);
        if (StrUtil.isNotBlank(reason)) {
            model.setRefundReason(reason);
        }
        request.setBizModel(model);

        AlipayTradeRefundResponse response;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("支付宝退款 API 异常，主动查询: orderNo={}", orderNo, e);
            queryAlipayRefundAndSync(refundRecord);
            refundRecord = lambdaQuery().eq(ERefundRecord::getId, refundRecord.getId()).one();
            if (refundRecord != null && refundRecord.getStatus() == RefundStatus.SUCCESS) {
                return buildRefundResultVO(refundRecord);
            }
            throw new BadRequestException("退款系统繁忙，请稍后查询退款状态");
        }

        if (!response.isSuccess()) {
            log.error("支付宝退款失败: orderNo={}, code={}, msg={}", orderNo, response.getCode(), response.getMsg());
            lambdaUpdate()
                    .set(ERefundRecord::getStatus, RefundStatus.FAILED)
                    .eq(ERefundRecord::getId, refundRecord.getId())
                    .update();
            refundRecord.setStatus(RefundStatus.FAILED);
            throw new BadRequestException("退款失败: " + response.getMsg());
        }

        if ("Y".equals(response.getFundChange())) {
            refundRecord.setRefundNo(response.getTradeNo());
            refundRecord.setStatus(RefundStatus.SUCCESS);
            if (response.getGmtRefundPay() != null) {
                refundRecord.setRefundTime(response.getGmtRefundPay().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            updateById(refundRecord);
            callbackOrder(orderNo, refundAmount);
        }
        // fundChange != "Y" 时保持 PROCESSING，等待异步通知或定时兜底

        return buildRefundResultVO(refundRecord);
    }

    @Override
    public RefundResultVO queryRefund(String outRequestNo) {
        ERefundRecord record = lambdaQuery()
                .eq(ERefundRecord::getOutRequestNo, outRequestNo)
                .one();
        if (record == null) {
            throw new BadRequestException("退款记录不存在");
        }
        if (record.getStatus() == RefundStatus.PROCESSING) {
            return queryAlipayRefundAndSync(record);
        }
        return buildRefundResultVO(record);
    }

    @Override
    @Transactional
    public String handleRefundNotify(Map<String, String> params) {
        log.info("收到支付宝退款异步通知: {}", params);
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params, alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(), alipayProperties.getSignType());
            if (!verified) {
                log.error("支付宝退款通知验签失败");
                return "failure";
            }
            AlipayNotifyDTO notify = mapToNotifyDTO(params);
            return processRefundNotify(notify) ? "success" : "failure";
        } catch (Exception e) {
            log.error("退款异步通知处理异常", e);
            return "failure";
        }
    }

    // ======================== private ========================

    private void callbackOrder(Long orderNo, Integer refundAmount) {
        try {
            R<Void> r = orderClient.refundCallback(orderNo, refundAmount);
            if (!r.success()) {
                log.warn("订单退款回调失败: orderNo={}, msg={}", orderNo, r.getMessage());
            }
        } catch (Exception e) {
            log.error("订单退款回调异常: orderNo={}", orderNo, e);
        }
    }

    private boolean processRefundNotify(AlipayNotifyDTO notify) {
        String tradeStatus = notify.getTradeStatus();
        if (!"REFUND_SUCCESS".equals(tradeStatus)) {
            return true;
        }

        ERefundRecord record = lambdaQuery()
                .eq(ERefundRecord::getPayNo, notify.getTradeNo())
                .eq(ERefundRecord::getStatus, RefundStatus.PROCESSING)
                .last("LIMIT 1")
                .one();
        if (record == null) {
            log.warn("退款通知无匹配记录: tradeNo={}", notify.getTradeNo());
            return true;
        }

        // 先回调 order-service（幂等），再写本地记录
        // 回调网络异常 → 返回 failure，支付宝会重发通知
        try {
            R<Void> r = orderClient.refundCallback(record.getOrderNo(), record.getRefundAmount());
            if (!r.success()) {
                log.warn("退款回调返回非成功: orderNo={}, msg={}", record.getOrderNo(), r.getMessage());
            }
        } catch (Exception e) {
            log.error("退款回调异常，将返回 failure 等待重试: orderNo={}", record.getOrderNo(), e);
            return false;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var update = lambdaUpdate()
                .set(ERefundRecord::getRefundNo, notify.getTradeNo())
                .set(ERefundRecord::getStatus, RefundStatus.SUCCESS)
                .eq(ERefundRecord::getId, record.getId())
                .eq(ERefundRecord::getStatus, RefundStatus.PROCESSING);
        if (StrUtil.isNotBlank(notify.getGmtPayment())) {
            update.set(ERefundRecord::getRefundTime, LocalDateTime.parse(notify.getGmtPayment(), fmt));
        }
        if (!update.update()) {
            log.info("退款记录已被其他线程更新: id={}", record.getId());
        }

        log.info("退款异步通知处理成功: orderNo={}, refundNo={}", record.getOrderNo(), notify.getTradeNo());
        return true;
    }

    @Transactional
    public RefundResultVO queryAlipayRefundAndSync(ERefundRecord record) {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setTradeNo(record.getPayNo());
        model.setOutRequestNo(record.getOutRequestNo());
        request.setBizModel(model);

        try {
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess() && "REFUND_SUCCESS".equals(response.getRefundStatus())) {
                boolean callbackOk = false;
                try {
                    R<Void> r = orderClient.refundCallback(record.getOrderNo(), record.getRefundAmount());
                    callbackOk = r.success();
                    if (!callbackOk) {
                        log.warn("退款查询同步回调失败（非成功响应）: orderNo={}, msg={}",
                                record.getOrderNo(), r.getMessage());
                    }
                } catch (Exception e) {
                    log.error("退款查询同步回调异常: orderNo={}", record.getOrderNo(), e);
                }

                if (!callbackOk) {
                    log.warn("退款回调未成功，保持 PROCESSING 等待下次对账重试: orderNo={}",
                            record.getOrderNo());
                    return buildRefundResultVO(record);
                }

                var update = lambdaUpdate()
                        .set(ERefundRecord::getRefundNo, response.getTradeNo())
                        .set(ERefundRecord::getStatus, RefundStatus.SUCCESS)
                        .eq(ERefundRecord::getId, record.getId())
                        .eq(ERefundRecord::getStatus, RefundStatus.PROCESSING);
                if (response.getGmtRefundPay() != null) {
                    update.set(ERefundRecord::getRefundTime,
                            response.getGmtRefundPay().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                }
                if (!update.update()) {
                    log.info("退款记录已被其他线程更新: id={}", record.getId());
                }
            }
        } catch (AlipayApiException e) {
            log.error("支付宝退款查询异常: outRequestNo={}", record.getOutRequestNo(), e);
        }
        return buildRefundResultVO(record);
    }

    private RefundResultVO buildRefundResultVO(ERefundRecord record) {
        return BeanUtils.copyBean(record, RefundResultVO.class, (src, target) -> {
            target.setStatus(src.getStatus().getValue());
            target.setStatusText(src.getStatus().getDesc());
        });
    }

    private AlipayNotifyDTO mapToNotifyDTO(Map<String, String> params) {
        AlipayNotifyDTO dto = BeanUtil.mapToBeanIgnoreCase(params, AlipayNotifyDTO.class, false);
        String totalAmount = params.get("total_amount");
        if (StrUtil.isNotBlank(totalAmount)) {
            dto.setTotalAmount(new BigDecimal(totalAmount));
        }
        return dto;
    }
}
