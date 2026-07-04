package com.ecommerce.product.service;

import com.ecommerce.product.domain.po.EProductSku;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.api.dto.SkuVO;

import java.util.List;

/**
 * <p>
 * 商品 SKU 表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
public interface IEProductSkuService extends IService<EProductSku> {

    List<SkuVO> getListByProductId(Long id);

    List<SkuVO> getListByProductIds(List<Long> productIds);
}
