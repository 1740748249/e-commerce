package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.product.domain.po.ECategory;
import com.ecommerce.product.domain.vo.CategoryVO;

import java.util.List;

public interface IECategoryService extends IService<ECategory> {

    List<CategoryVO> getCategoryList();
}
