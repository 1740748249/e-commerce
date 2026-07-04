package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.vo.ShopApplicationVO;
import com.ecommerce.product.service.IEShopService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Api(tags = "管理员—店铺相关接口")
@RequiredArgsConstructor
public class AdminShopController {
    private final IEShopService shopService;

    @GetMapping("/shops/pending")
    public R<PageDTO<ShopApplicationVO>> getPendingShops(PageQuery query) {
        return shopService.getPendingShops(query);
    }

    @PutMapping("/shops/{shopId}/approve")
    public R<Void> approveShop(@PathVariable Long shopId,
                               @RequestParam Boolean approved) {
        return shopService.approveShop(shopId, approved);
    }

    @GetMapping("/shops/pending/count")
    public R<Long> getPendingShopCount() {
        return R.ok(shopService.getPendingShopCount());
    }
}
