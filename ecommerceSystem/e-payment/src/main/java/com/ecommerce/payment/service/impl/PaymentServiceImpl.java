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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    private static final int ORDER_STATUS_PENDING_PAYMENT = 0;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public String pay(Long orderNo) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }
        //跨服务查询订单基本信息
        R<OrderBasicDTO> r = orderClient.getOrderBasic(orderNo);
        if (r == null || !r.success() || r.getData() == null) {
            throw new BadRequestException("订单不存在");
        }
        //获取响应数据
        OrderBasicDTO order = r.getData();
        //校验用户权限，是否与订单用户一致
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("订单不属于当前用户");
        }
        //检验订单状态是否为待支付
        if (order.getStatus() != ORDER_STATUS_PENDING_PAYMENT) {
            throw new BadRequestException("订单状态不允许支付");
        }

        // 秒杀订单分支
        if (order.getFlashSaleOrderId() != null) {
            //远程查询秒杀订单状态
            R<Integer> statusR = productClient.getFlashOrderStatus(order.getFlashSaleOrderId());
            //如果响应失败，则抛出异常，不给用户继续支付
            if (!statusR.success() || statusR.getData() == null) {
                throw new BadRequestException("秒杀订单状态查询失败，请稍后重试");
            }
            //当秒杀订单状态不为待支付，则抛出异常，不允许继续支付
            if (statusR.getData() != 0) { // 0 = PENDING_PAYMENT
                throw new BadRequestException("秒杀订单已超时，请重新下单");
            }
        }
       //计算实际支付金额
        int payAmount = order.getTotalAmount() - order.getDiscountAmount();
        //如果支付金额小于0，则抛出异常，不允许继续支付
        if (payAmount <= 0) {
            throw new BadRequestException("订单金额异常");
        }
        //查询该订单是否有支付记录
        EPaymentRecord existRecord = lambdaQuery()
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .one();
        //支付记录不为空
        if (existRecord != null) {
            //如果支付记录为已支付，则抛出异常，不允许继续支付
            if (existRecord.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("该订单已支付");
            }
            //如果支付记录为已关闭，则删除该支付记录，开启新的支付
            if (existRecord.getStatus() == PaymentStatus.CLOSED) {
                removeById(existRecord.getId());
            }
            else {
                // PENDING → 主动查支付宝确认实际状态
                AlipayTradeQueryResponse queryResp = queryAlipayTrade(orderNo.toString());
                //支付宝查询成功
                if (queryResp != null && queryResp.isSuccess()) {
                    //获取订单实际支付状态
                    String ts = queryResp.getTradeStatus();
                    //实际支付状态为支付成功或者支付完成
                    if ("TRADE_SUCCESS".equals(ts) || "TRADE_FINISHED".equals(ts)) {
                        //获取订单实际支付时间
                        LocalDateTime payTime = queryResp.getSendPayDate() != null
                                ? queryResp.getSendPayDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                                : LocalDateTime.now();
                        try {
                            //重新发起成功支付回调
                            orderClient.payCallback(orderNo, queryResp.getTradeNo(), payTime.format(FMT));
                        } catch (Exception feignEx) {
                            log.error("同步支付回调失败 orderNo={}", orderNo, feignEx);
                        }
                        // 1. 创建独立事务模板（基于当前事务管理器）
                        TransactionTemplate tt = new TransactionTemplate(transactionManager);
                        // 2. 设置为 REQUIRES_NEW：挂起外围事务，开启全新独立事务
                        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        // 3. 在新事务中执行更新，即使外围事务回滚也不受影响
                        tt.execute(txStatus -> {
                            //支付回调成功，更新支付记录为支付成功状态
                            lambdaUpdate()
                                    .set(EPaymentRecord::getPayNo, queryResp.getTradeNo())
                                    .set(EPaymentRecord::getStatus, PaymentStatus.SUCCESS)
                                    .set(EPaymentRecord::getPayTime, payTime)
                                    .eq(EPaymentRecord::getId, existRecord.getId())
                                    .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)//乐观锁防竟态
                                    .update();
                            return null;
                        });
                        throw new BadRequestException("该订单已支付，请勿重复支付");
                    }
                    //订单实际支付状态为待支付，直接删除旧记录，开启新的支付记录
                    removeById(existRecord.getId());
                }
                else {
                    //主动查询支付宝，结果支付宝没给结果（可能网络波动）
                    throw new BadRequestException("该订单已有支付处理中，请稍后重试");
                }
            }
        }
        //封装支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //设置支付宝通知回调地址
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        //设置支付宝支付页面跳转地址
        request.setReturnUrl(alipayProperties.getReturnUrl() + "?orderNo=" + orderNo);
        //封装支付参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        //设置订单号
        model.setOutTradeNo(orderNo.toString());
        model.setTotalAmount(BigDecimal.valueOf(payAmount, 2).toPlainString());
        //设置支付界面支付主题(订单+orderId)
        model.setSubject(order.getSubject());
        //设置支付产品
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        //设置业务参数
        request.setBizModel(model);

        try {
            //发起支付
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (!response.isSuccess()) {
                log.error("支付宝下单失败: orderNo={}, code={}, msg={}", orderNo, response.getCode(), response.getMsg());
                throw new BadRequestException("支付发起失败: " + response.getMsg());
            }
            //创建支付记录
            EPaymentRecord record = new EPaymentRecord();
            //关联订单号
            record.setOrderNo(orderNo);
            //支付用户ID
            record.setUserId(userId);
            //支付金额
            record.setTotalAmount(payAmount);
            //支付状态
            record.setStatus(PaymentStatus.PENDING);
            //保存落库
            save(record);
            //返回支付宝页面
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

    @Override
    public boolean checkAndSyncByOrderNo(Long orderNo) {
        EPaymentRecord record = lambdaQuery()
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .last("LIMIT 1")
                .one();

        // 无支付记录 → 查支付宝兜底（pageExecute 可能成功但 save 失败）
        if (record == null) {
            AlipayTradeQueryResponse resp = queryAlipayTrade(orderNo.toString());
            if (resp != null && resp.isSuccess()
                    && ("TRADE_SUCCESS".equals(resp.getTradeStatus()) || "TRADE_FINISHED".equals(resp.getTradeStatus()))) {
                return syncPaidFromAlipay(orderNo, resp);
            }
            return false;
        }

        if (record.getStatus() == PaymentStatus.SUCCESS) {
            // 支付记录已成功，补推回调（防止订单侧未同步）
            try {
                orderClient.payCallback(orderNo, record.getPayNo(),
                        record.getPayTime() != null ? record.getPayTime().format(FMT) : LocalDateTime.now().format(FMT));
            } catch (Exception e) {
                log.error("checkAndSync 补推回调异常: orderNo={}", orderNo, e);
            }
            return true;
        }

        if (record.getStatus() == PaymentStatus.PENDING) {
            AlipayTradeQueryResponse resp = queryAlipayTrade(orderNo.toString());
            if (resp != null && resp.isSuccess()
                    && ("TRADE_SUCCESS".equals(resp.getTradeStatus()) || "TRADE_FINISHED".equals(resp.getTradeStatus()))) {
                return syncPaidFromAlipay(orderNo, resp);
            }
            // 未支付，返回 false 让 order-service 继续取消
            return false;
        }

        // CLOSED 等其他状态
        return false;
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

    private boolean syncPaidFromAlipay(Long orderNo, AlipayTradeQueryResponse resp) {
        LocalDateTime payTime = resp.getSendPayDate() != null
                ? resp.getSendPayDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();
        try {
            R<Void> callbackR = orderClient.payCallback(orderNo, resp.getTradeNo(), payTime.format(FMT));
            if (!callbackR.success()) {
                log.warn("checkAndSync 回调返回非成功: orderNo={}, msg={}", orderNo, callbackR.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("checkAndSync 回调异常: orderNo={}", orderNo, e);
            return false;
        }

        // 尝试更新已有 PENDING 记录
        boolean updated = lambdaUpdate()
                .set(EPaymentRecord::getPayNo, resp.getTradeNo())
                .set(EPaymentRecord::getStatus, PaymentStatus.SUCCESS)
                .set(EPaymentRecord::getPayTime, payTime)
                .eq(EPaymentRecord::getOrderNo, orderNo)
                .eq(EPaymentRecord::getStatus, PaymentStatus.PENDING)
                .update();

        // 无 PENDING 记录则新建（覆盖 pageExecute 成功但 save 失败的场景）
        if (!updated) {
            int totalAmount = 0;
            if (resp.getTotalAmount() != null) {
                totalAmount = new BigDecimal(resp.getTotalAmount()).movePointRight(2).intValue();
            }
            EPaymentRecord newRecord = new EPaymentRecord();
            newRecord.setOrderNo(orderNo);
            newRecord.setUserId(fetchUserIdByOrder(orderNo));
            newRecord.setPayNo(resp.getTradeNo());
            newRecord.setTotalAmount(totalAmount);
            newRecord.setStatus(PaymentStatus.SUCCESS);
            newRecord.setPayTime(payTime);
            save(newRecord);
        }
        log.info("checkAndSync 主动同步支付成功: orderNo={}, payNo={}", orderNo, resp.getTradeNo());
        return true;
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
