package com.ecommerce.order.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.order.domain.dto.CouponCreateDTO;
import com.ecommerce.order.query.CouponPageQuery;
import com.ecommerce.order.domain.vo.CouponVO;
import com.ecommerce.order.service.IECouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 优惠券模板表 前端控制器（管理员）
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class ECouponController {

    private final IECouponService couponService;

    @GetMapping
    public R<PageDTO<CouponVO>> page(CouponPageQuery query) {
        return couponService.page(query);
    }

    @GetMapping("/{id}")
    public R<CouponVO> detail(@PathVariable Long id) {
        return couponService.detail(id);
    }

    @PostMapping
    public R<CouponVO> create(@Valid @RequestBody CouponCreateDTO dto) {
        return couponService.create(dto);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CouponCreateDTO dto) {
        return couponService.update(id, dto);
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return couponService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return couponService.delete(id);
    }
}
