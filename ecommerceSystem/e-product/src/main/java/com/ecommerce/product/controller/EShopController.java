package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.ShopApplyDTO;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.product.domain.vo.ShopVO;
import com.ecommerce.product.service.IEShopService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/shops")
@Slf4j
@Api(tags = "店铺相关接口")
@RequiredArgsConstructor
public class EShopController {
    private final IEShopService shopService;

    @PostMapping("/apply")
    public R<Void> apply(@Valid @RequestBody ShopApplyDTO dto) {
        return shopService.apply(dto);
    }

    @GetMapping("/me")
    public R<ShopVO> getMyShop() {
        return shopService.getMyShop();
    }

    @GetMapping
    public R<List<ShopVO>> list() {
        return R.ok(shopService.getShopList());
    }

    @GetMapping("/{shopId}/products")
    public R<PageDTO<ProductVO>> shopProducts(@PathVariable Long shopId,
                                              PageQuery query,
                                              @RequestParam(defaultValue = "default") String sort) {
        return shopService.shopProducts(shopId, query, sort);
    }
}
