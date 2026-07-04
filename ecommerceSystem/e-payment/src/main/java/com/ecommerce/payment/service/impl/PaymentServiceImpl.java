package com.ecommerce.payment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.payment.config.AlipayProperties;
import com.ecommerce.payment.domain.dto.AlipayNotifyDTO;
import com.ecommerce.payment.domain.po.EPaymentRecord;
import com.ecommerce.payment.domain.vo.PayResultVO;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.mapper.EPaymentRecordMapper;
import com.ecommerce.payment.service.IPaymentService;
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
public class PaymentServiceImpl extends ServiceImpl<EPaymentRecordMapper, EPaymentRecord> implements IPaymentService {

    private final AlipayClient alipayClient;
    private final AlipayProperties alipayProperties;
    private final OrderClient orderClient;
    private final ProductClient productClient;

    private static final int ORDER_STATUS_PENDING_PAYMENT = 0;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public String pay(Long orderNo) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }
        R<OrderBasicDTO> r = orderClient.getOrderBasic(orderNo);
        if (r == null || !r.success() || r.getData() == null) {
            throw new BadRequestException("订单不存在");
        }
        OrderBasicDTO order = r.getData();
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("订单不属于当前用户");
        }
        if (order.getStatus() != ORDER_STATUS_PENDING_PAYMENT) {
            throw new BadRequestException("订单状态不允许支付");
        }

        // 秒杀订单：校验闪购订单状态，防止支付已被超时释放库存的订单
        if (order.getFlashSaleOrderId() != null) {
            R<Integer> statusR = productClient.getFlashOrderStatus(order.getFlashSaleOrderId());
            if (!statusR.success() || statusR.getData() == null) {
                throw new BadRequestException("秒杀订单状态查询失败，请稍后重试");
            }
            if (statusR.getData() != 0) { // 0 = PENDING_PAYMENT
                throw new BadRequestException("秒杀订单已超时，请重新下单");
            }
        }

        int payAmount = order.getTotalAmount() - order.getDiscountAmount();
        if (payAmount <= 0) {
            throw new BadRequestException("订单金额异常");
        }

        EPaymentRecord existRecord = lambdaQuery()
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .last("LIMIT 1")
                .one();
        if (existRecord != null) {
            if (existRecord.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("该订单已支付");
            }
            if (existRecord.getStatus() == PaymentStatus.CLOSED) {
                removeById(existRecord.getId());
            } else {
                // PENDING → 主动查支付宝确认实际状态
                AlipayTradeQueryResponse queryResp = queryAlipayTrade(orderNo.toString());
                if (queryResp != null && queryResp.isSuccess()) {
                    String ts = queryResp.getTradeStatus();
                    if ("TRADE_SUCCESS".equals(ts) || "TRADE_FINISHED".equals(ts)) {
                        LocalDateTime payTime = queryResp.getSendPayDate() != null
                                ? queryResp.getSendPayDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                                : LocalDateTime.now();
                        try {
                            orderClient.payCallback(orderNo, queryResp.getTradeNo(), payTime.format(FMT));
                        } catch (Exception feignEx) {
                            log.error("主动查单后回调异常: orderNo={}", orderNo, feignEx);
                        }
                        lambdaUpdate()
                                .set(EPaymentRecord::getPayNo, queryResp.getTradeNo())
                                .set(EPaymentRecord::getStatus, PaymentStatus.SUCCESS)
                                .set(EPaymentRecord::getPayTime, payTime)
                                .eq(EPaymentRecord::getId, existRecord.getId())
                                .update();
                        throw new BadRequestException("该订单已支付，请勿重复支付");
                    }
                    removeById(existRecord.getId());
                } else {
                    throw new BadRequestException("该订单已有支付处理中，请稍后重试");
                }
            }
        }

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl() + "?orderNo=" + orderNo);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(orderNo.toString());
        model.setTotalAmount(BigDecimal.valueOf(payAmount, 2).toPlainString());
        model.setSubject(order.getSubject());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                log.error("支付宝下单失败: orderNo={}, code={}, msg={}", orderNo, response.getCode(), response.getMsg());
                throw new BadRequestException("支付发起失败: " + response.getMsg());
            }
            EPaymentRecord record = new EPaymentRecord();
            record.setOrderNo(orderNo);
            record.setUserId(userId);
            record.setTotalAmount(payAmount);
            record.setStatus(PaymentStatus.PENDING);
            save(record);
            return response.getBody();
        } catch (AlipayApiException e) {
            log.error("支付宝 API 异常: orderNo={}", orderNo, e);
            throw new BadRequestException("支付系统繁忙，请稍后重试");
        }
    }

    @Override
    @Transactional
    public String handleNotify(Map<String, String> params) {
        log.info("收到支付宝支付异步通知: {}", params);
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params, alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(), alipayProperties.getSignType());
            if (!verified) {
                log.error("支付宝支付通知验签失败");
                return "failure";
            }
            AlipayNotifyDTO notify = mapToNotifyDTO(params);
            return processPayNotify(notify) ? "success" : "failure";
        } catch (Exception e) {
            log.error("支付异步通知处理异常", e);
            return "failure";
        }
    }

    @Override
    public PayResultVO queryStatus(Long orderNo) {
        EPaymentRecord record = lambdaQuery()
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .orderByDesc(EPaymentRecord::getCreateTime)
                .last("LIMIT 1")
                .one();
        if (record == null) {
            return PayResultVO.builder().orderNo(orderNo)
                    .status(PaymentStatus.PENDING.getValue())
                    .statusText(PaymentStatus.PENDING.getDesc()).build();
        }
        return buildResultVO(record);
    }

    // ======================== private ========================

    private boolean processPayNotify(AlipayNotifyDTO notify) {
        String orderNoStr = notify.getOutTradeNo();
        if (StrUtil.isBlank(orderNoStr)) {
            log.warn("异步通知缺少 out_trade_no");
            return false;
        }
        Long orderNo;
        try {
            orderNo = Long.valueOf(orderNoStr);
        } catch (NumberFormatException e) {
            log.error("异步通知 out_trade_no 格式错误: {}", orderNoStr);
            return false;
        }

        String tradeStatus = notify.getTradeStatus();
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("异步通知非终态: orderNo={}, tradeStatus={}", orderNo, tradeStatus);
            return true;
        }

        BigDecimal notifyAmount = notify.getTotalAmount();
        if (notifyAmount == null) {
            log.error("异步通知缺少交易金额: orderNo={}", orderNo);
            return false;
        }
        int alipayAmountFen = notifyAmount.movePointRight(2).intValue();

        String gmtPayment = notify.getGmtPayment();
        if (StrUtil.isBlank(gmtPayment)) {
            log.error("异步通知缺少 gmt_payment: orderNo={}", orderNo);
            return false;
        }

        // 幂等：按支付宝交易号查，已处理的直接返回成功
        EPaymentRecord record = lambdaQuery()
                .eq(EPaymentRecord::getPayNo, notify.getTradeNo())
                .last("LIMIT 1")
                .one();
        if (record != null && record.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }
        if (record == null) {
            record = lambdaQuery()
                    .eq(EPaymentRecord::getOrderNo, orderNo)
                    .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)
                    .last("LIMIT 1")
                    .one();
        }

        if (record != null && !record.getTotalAmount().equals(alipayAmountFen)) {
            log.error("支付金额不匹配: orderNo={}, local={}, alipay={}", orderNo, record.getTotalAmount(), alipayAmountFen);
            return false;
        }

        // 先回调 order-service（幂等），再写本地记录
        // 回调网络异常 → 返回 failure，支付宝会重发通知
        boolean orderTerminal = false;
        try {
            R<Void> callbackR = orderClient.payCallback(orderNo, notify.getTradeNo(), gmtPayment);
            if (!callbackR.success()) {
                log.warn("支付回调返回非成功（订单状态可能已变更）: orderNo={}, msg={}", orderNo, callbackR.getMessage());
                orderTerminal = isOrderTerminal(orderNo);
            }
        } catch (Exception e) {
            log.error("Feign 调用 order-service 支付回调异常，将返回 failure 等待重试: orderNo={}", orderNo, e);
            return false;
        }

        PaymentStatus targetStatus = orderTerminal ? PaymentStatus.CLOSED : PaymentStatus.SUCCESS;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime payTime = LocalDateTime.parse(gmtPayment, fmt);
        if (record == null) {
            record = new EPaymentRecord();
            record.setOrderNo(orderNo);
            record.setUserId(fetchUserIdByOrder(orderNo));
            record.setPayNo(notify.getTradeNo());
            record.setTotalAmount(alipayAmountFen);
            record.setStatus(targetStatus);
            record.setPayTime(payTime);
            save(record);
        } else {
            boolean updated = lambdaUpdate()
                    .set(EPaymentRecord::getPayNo, notify.getTradeNo())
                    .set(EPaymentRecord::getStatus, targetStatus)
                    .set(EPaymentRecord::getPayTime, payTime)
                    .eq(EPaymentRecord::getId, record.getId())
                    .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)
                    .update();
            if (!updated) {
                log.info("支付记录已被其他线程更新: orderNo={}", orderNo);
            }
        }
        log.info("支付异步通知处理成功: orderNo={}, payNo={}, status={}", orderNo, notify.getTradeNo(), targetStatus);
        return true;
    }

    private PayResultVO buildResultVO(EPaymentRecord record) {
        return BeanUtils.copyBean(record, PayResultVO.class, (src, target) -> {
            target.setStatus(src.getStatus().getValue());
            target.setStatusText(src.getStatus().getDesc());
        });
    }

    private Long fetchUserIdByOrder(Long orderNo) {
        try {
            R<OrderBasicDTO> r = orderClient.getOrderBasic(orderNo);
            if (r.success() && r.getData() != null) {
                return r.getData().getUserId();
            }
        } catch (Exception e) {
            log.warn("获取订单用户ID失败: orderNo={}", orderNo, e);
        }
        return 0L;
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

    private AlipayTradeQueryResponse queryAlipayTrade(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);
        try {
            return alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("支付宝查单异常: outTradeNo={}", outTradeNo, e);
            return null;
        }
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
