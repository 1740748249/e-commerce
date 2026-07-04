package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.ShopApplyDTO;
import com.ecommerce.product.domain.po.EShop;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.product.domain.vo.ShopApplicationVO;
import com.ecommerce.product.domain.vo.ShopVO;

import java.util.List;

public interface IEShopService extends IService<EShop> {

    R<Void> apply(ShopApplyDTO dto);

    R<ShopVO> getMyShop();

    List<ShopVO> getShopList();

    R<PageDTO<ProductVO>> shopProducts(Long shopId, PageQuery query, String sort);

    R<PageDTO<ShopApplicationVO>> getPendingShops(PageQuery query);

    R<Void> approveShop(Long shopId, Boolean approved);

    Long getPendingShopCount();
}
