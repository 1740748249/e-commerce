package com.ecommerce.order.service;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.order.domain.dto.CouponCreateDTO;
import com.ecommerce.order.domain.po.ECoupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.order.query.CouponPageQuery;
import com.ecommerce.order.domain.vo.CouponVO;

import java.util.List;

/**
 * <p>
 * 优惠券模板表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
public interface IECouponService extends IService<ECoupon> {

    R<PageDTO<CouponVO>> page(CouponPageQuery query);

    R<CouponVO> detail(Long id);

    R<CouponVO> create(CouponCreateDTO dto);

    R<Void> update(Long id, CouponCreateDTO dto);

    R<Void> updateStatus(Long id, Integer status);

    R<Void> delete(Long id);
}
