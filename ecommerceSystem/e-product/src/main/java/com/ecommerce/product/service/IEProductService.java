package com.ecommerce.product.service;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.ProductCreateDTO;
import com.ecommerce.product.domain.dto.ProductUpdateDTO;
import com.ecommerce.product.domain.po.EProduct;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.product.domain.query.ProductPageQuery;
import com.ecommerce.product.domain.vo.ProductDetailVO;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.product.enums.ProductStatus;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 商品 SPU 表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
public interface IEProductService extends IService<EProduct> {

    R<PageDTO<ProductVO>> list(ProductPageQuery query);

    R<ProductDetailVO> detail(Long id);

    R<Void> create(ProductCreateDTO dto);

    R<Void> update(Long id, ProductUpdateDTO dto);

    R<Void> updateStatus(Long id, ProductStatus status);

    R<PageDTO<ProductVO>> myProducts(PageQuery query);

    R<List<ProductVO>> getDetailsByIds(Set<Long> ids);
}
