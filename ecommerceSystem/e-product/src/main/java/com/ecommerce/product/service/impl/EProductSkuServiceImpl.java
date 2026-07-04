package com.ecommerce.product.service.impl;

import cn.hutool.json.JSONObject;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.JsonUtils;
import com.ecommerce.product.domain.po.EProductSku;
import com.ecommerce.api.dto.SkuVO;
import com.ecommerce.product.enums.SkuStatus;
import com.ecommerce.product.mapper.EProductSkuMapper;
import com.ecommerce.product.service.IEProductSkuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品 SKU 表 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Service
public class EProductSkuServiceImpl extends ServiceImpl<EProductSkuMapper, EProductSku> implements IEProductSkuService {

    @Override
    public List<SkuVO> getListByProductIds(List<Long> productIds) {
        if (CollUtils.isEmpty(productIds)) {
            return CollUtils.emptyList();
        }
        List<EProductSku> skuList = lambdaQuery()
                .in(EProductSku::getProductId, productIds)
                .eq(EProductSku::getStatus, SkuStatus.ENABLED)
                .list();
        if (CollUtils.isEmpty(skuList)) {
            return CollUtils.emptyList();
        }
        return skuList.stream().map(entity -> {
            SkuVO skuVO = new SkuVO();
            ArrayList<SkuVO.SpecVO> specsList = new ArrayList<>();
            BeanUtils.copyProperties(entity, skuVO, "specs");
            JSONObject jsonObject = JsonUtils.parseObj(entity.getSpecs());
            jsonObject.forEach((key, value) -> {
                SkuVO.SpecVO specVO = new SkuVO.SpecVO();
                specVO.setName(key);
                specVO.setValue(value.toString());
                specsList.add(specVO);
            });
            skuVO.setSpecs(specsList);
            return skuVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SkuVO> getListByProductId(Long id) {
        return getListByProductIds(Collections.singletonList(id));
    }
}
