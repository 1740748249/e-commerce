package com.ecommerce.product.service.impl;

import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.product.domain.po.EProduct;
import com.ecommerce.product.domain.po.EShop;
import com.ecommerce.product.domain.vo.RankingItemVO;
import com.ecommerce.product.domain.vo.ShopVO;
import com.ecommerce.product.mapper.EProductMapper;
import com.ecommerce.product.mapper.EShopMapper;
import com.ecommerce.product.service.IRankingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ecommerce.product.constants.CacheConstants.*;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl extends ServiceImpl<EProductMapper, EProduct> implements IRankingService {
    private final EShopMapper shopMapper;
    private final CacheService cacheService;

    @Override
    public List<RankingItemVO> getRanking(int limit) {
        String cacheKey = RANKING_KEY_PREFIX + ":" + limit;
        return cacheService.getOrLoadList(cacheKey, RANKING_TTL, RankingItemVO.class, () -> {
            // 1. 查询 TOP N 商品（走 idx_sales 索引，LIMIT 回表极少量）
            List<EProduct> products = baseMapper.selectTopBySales(limit);
            if (CollUtils.isEmpty(products)) {
                return CollUtils.emptyList();
            }

            // 2. 取店铺名缓存（SHOP_ALL_KEY 已被 getShopList 预热，大概率命中）
            Map<String, ShopVO> shopMap = cacheService.hGetAllOrLoad(
                    SHOP_ALL_KEY, SHOP_TTL, ShopVO.class, () -> {
                        List<EShop> shops = shopMapper.selectList(null);
                        if (CollUtils.isEmpty(shops)) {
                            return CollUtils.emptyMap();
                        }
                        return shops.stream().collect(Collectors.toMap(
                                s -> s.getId().toString(),
                                s -> BeanUtils.copyBean(s, ShopVO.class),
                                (a, b) -> a));
                    });

            // 3. 组装 VO
            List<RankingItemVO> result = new ArrayList<>(products.size());
            for (int i = 0; i < products.size(); i++) {
                EProduct p = products.get(i);
                RankingItemVO vo = new RankingItemVO();
                vo.setRank(i + 1);
                vo.setProductId(p.getId());
                vo.setProductName(p.getName());
                vo.setProductImage(p.getImage());
                vo.setMinPrice(p.getMinPrice());
                vo.setSales(p.getSales());
                ShopVO shop = shopMap.get(String.valueOf(p.getShopId()));
                vo.setShopName(shop != null ? shop.getName() : "");
                result.add(vo);
            }
            return result;
        });
    }
}
