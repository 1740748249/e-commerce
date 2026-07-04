package com.ecommerce.order.service;

import com.ecommerce.common.domain.R;
import com.ecommerce.order.domain.po.EUserCoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.order.domain.vo.CouponVO;
import com.ecommerce.order.domain.vo.UserCouponVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户优惠券表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
public interface IEUserCouponService extends IService<EUserCoupon> {

    R<List<CouponVO>> listAvailable();

    R<Void> claim(Long couponId);

    R<Map<String, List<UserCouponVO>>> myCoupons();
}
