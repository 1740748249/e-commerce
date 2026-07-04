package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.vo.CategoryVO;
import com.ecommerce.product.service.IECategoryService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Api(tags = "商品分类相关接口")
@RequiredArgsConstructor
public class ECategoryController {
    private final IECategoryService categoryService;

    @GetMapping
    public R<List<CategoryVO>> list() {
        return R.ok(categoryService.getCategoryList());
    }
}
