package com.ecommerce.product.controller;


import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.ProductCreateDTO;
import com.ecommerce.product.domain.dto.ProductUpdateDTO;
import com.ecommerce.product.domain.query.ProductPageQuery;
import com.ecommerce.product.domain.vo.ProductDetailVO;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.service.IEProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 商品 SPU 表 前端控制器
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@RestController
@RequestMapping("/products")
@Slf4j
@RequiredArgsConstructor
public class EProductController {
    private final IEProductService productService;

    @GetMapping
    public R<PageDTO<ProductVO>> list(ProductPageQuery query) {
        return productService.list(query);
    }

    @GetMapping("/{id}")
    public R<ProductDetailVO> detail(@PathVariable Long id) {
        return productService.detail(id);
    }

    /**
     * 发布商品
     * @param dto
     * @return
     */
    @PostMapping
    public R<Void> create(@Valid @RequestBody ProductCreateDTO dto) {
        return productService.create(dto);
    }

    /**
     * 编辑修改商品
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        return productService.update(id, dto);
    }

    /**
     * 商家上下架商品
     * @param id
     * @param status
     * @return
     */

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam ProductStatus status) {
        return productService.updateStatus(id, status);
    }

    /**
     * 商家 - 获取自己的商品列表
     * @param query
     * @return
     */
    @GetMapping("/my")
    public R<PageDTO<ProductVO>> myProducts(PageQuery query) {
        return productService.myProducts(query);
    }

    /**
     * 根据产品ids批量查询产品信息
     */
    @GetMapping("/list")
    public R<List<ProductVO>> getDetailsByIds(@RequestParam("ids") Set<Long> ids) {
        return productService.getDetailsByIds(ids);
    }
}
