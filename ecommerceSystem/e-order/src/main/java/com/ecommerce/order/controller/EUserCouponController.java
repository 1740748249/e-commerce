package com.ecommerce.order.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.order.domain.vo.CouponVO;
import com.ecommerce.order.domain.vo.UserCouponVO;
import com.ecommerce.order.service.IEUserCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户优惠券表 前端控制器
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class EUserCouponController {

    private final IEUserCouponService userCouponService;

    @GetMapping
    public R<List<CouponVO>> listAvailable() {
        return userCouponService.listAvailable();
    }

    @PostMapping("/{couponId}/claim")
    public R<Void> claim(@PathVariable Long couponId) {
        return userCouponService.claim(couponId);
    }

    @GetMapping("/my")
    public R<Map<String, List<UserCouponVO>>> myCoupons() {
        return userCouponService.myCoupons();
    }
}
