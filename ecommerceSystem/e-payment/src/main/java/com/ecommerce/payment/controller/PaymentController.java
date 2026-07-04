package com.ecommerce.payment.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.payment.domain.dto.RefundRequestDTO;
import com.ecommerce.payment.domain.vo.PayResultVO;
import com.ecommerce.payment.domain.vo.RefundResultVO;
import com.ecommerce.payment.service.IPaymentService;
import com.ecommerce.payment.service.IRefundService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@Api(tags = "支付服务相关接口")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;
    private final IRefundService refundService;

    @PostMapping(value = "/pay", produces = MediaType.TEXT_HTML_VALUE)
    public String pay(@RequestParam Long orderNo) {
        return paymentService.pay(orderNo);
    }

    @PostMapping("/notify")
    public String payNotify(@RequestParam Map<String, String> params) {
        return paymentService.handleNotify(params);
    }

    @GetMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE)
    public String payReturn(@RequestParam Long orderNo) {
        paymentService.queryStatus(orderNo);
        // 此页面在 iframe 中加载，通过 postMessage 通知父窗口（前端 SPA）
        // 父窗口收到消息后执行 router.push('/pay-result')
        return "<!DOCTYPE html>"
                + "<html><head><meta charset=\"UTF-8\"></head>"
                + "<body><script>"
                + "window.parent.postMessage({type:'ALIPAY_RETURN',orderNo:'" + orderNo + "'},'*');"
                + "</script></body></html>";
    }

    @GetMapping("/status/{orderNo}")
    public R<PayResultVO> status(@PathVariable Long orderNo) {
        return R.ok(paymentService.queryStatus(orderNo));
    }

    @PostMapping("/refund")
    public R<RefundResultVO> refund(@Valid @RequestBody RefundRequestDTO dto) {
        return R.ok(refundService.refund(dto.getOrderNo(), dto.getRefundAmount(), dto.getReason()));
    }

    @GetMapping("/refund/{outRequestNo}")
    public R<RefundResultVO> refundStatus(@PathVariable String outRequestNo) {
        return R.ok(refundService.queryRefund(outRequestNo));
    }

    @PostMapping("/refund/notify")
    public String refundNotify(@RequestParam Map<String, String> params) {
        return refundService.handleRefundNotify(params);
    }
}
