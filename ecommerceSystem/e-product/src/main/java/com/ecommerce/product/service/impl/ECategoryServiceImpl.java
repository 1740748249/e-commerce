package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.product.constants.CacheConstants;
import com.ecommerce.product.domain.po.ECategory;
import com.ecommerce.product.domain.vo.CategoryVO;
import com.ecommerce.product.mapper.ECategoryMapper;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.product.service.IECategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

import static com.ecommerce.product.constants.CacheConstants.CATEGORY_ALL_KEY;
import static com.ecommerce.product.constants.CacheConstants.CATEGORY_TTL;

@Service
@RequiredArgsConstructor
public class ECategoryServiceImpl extends ServiceImpl<ECategoryMapper, ECategory> implements IECategoryService {
    private final CacheService cacheService;


    @Override
    public List<CategoryVO> getCategoryList() {
        //先查询缓存
       return cacheService.getOrLoadList(CATEGORY_ALL_KEY,CATEGORY_TTL,CategoryVO.class,()->{
            //查询数据库
            List<ECategory> list = list();
            return BeanUtils.copyList(list, CategoryVO.class);
        });
    }
}
