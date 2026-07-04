package com.ecommerce.product.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.utils.*;
import com.ecommerce.product.domain.dto.ProductCreateDTO;
import com.ecommerce.product.domain.dto.ProductUpdateDTO;
import com.ecommerce.product.domain.dto.SkuCreateDTO;
import com.ecommerce.product.domain.dto.SpecDTO;
import com.ecommerce.product.domain.po.ECategory;
import com.ecommerce.product.domain.po.EProduct;
import com.ecommerce.product.domain.po.EProductSku;
import com.ecommerce.product.domain.po.EShop;
import com.ecommerce.product.domain.query.ProductPageQuery;
import com.ecommerce.product.domain.vo.*;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.api.dto.SkuVO;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.enums.ShopStatus;
import com.ecommerce.product.enums.SkuStatus;
import com.ecommerce.product.mapper.EProductMapper;
import com.ecommerce.product.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.product.constants.CacheConstants.*;

/**
 * <p>
 * 商品 SPU 表 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EProductServiceImpl extends ServiceImpl<EProductMapper, EProduct> implements IEProductService {
    private final IEShopService shopService;
    private final IECategoryService categoryService;
    private final IEProductSkuService productSkuService;
    private final CacheService cacheService;
    private final Snowflake snowflake;

    @Override
    public R<PageDTO<ProductVO>> list(ProductPageQuery query) {
        //TODO:后期通过elasticsearch实现
        List<Long> shopIds = null;
        try {
            shopIds = getApprovedShop();
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
        //构建分页查询参数带排序条件
        Page<EProduct> mpage=new Page<>(query.getPage(),query.getSize());
        String sortName = query.getSort();
        if(sortName==null){
            mpage.addOrder(OrderItem.desc("create_time"));
        }
        else if(sortName.equals("price_asc")){
            mpage.addOrder(OrderItem.asc("min_price"));
        } else if (sortName.equals("price_desc")) {
            mpage.addOrder(OrderItem.desc("min_price"));
        }
        else{
            //走默认获取选择总销量排序
            mpage.addOrder(OrderItem.desc("sales"));
        }
        Page<EProduct> page = lambdaQuery()
                .eq(query.getCategoryId() != null, EProduct::getCategoryId, query.getCategoryId())
                .eq(EProduct::getStatus, ProductStatus.ON_SHELF)
                .in(EProduct::getShopId, shopIds)
                .like(StringUtils.isNotBlank(query.getKeyword()),EProduct::getName, query.getKeyword())
                .page(mpage);
        List<EProduct> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            //列表为空
            return R.ok(PageDTO.empty(page));
        }
        //转换VO输出
        List<ProductVO> productVOS = BeanUtils.copyToList(records, ProductVO.class);
        return R.ok(PageDTO.of(page, productVOS));

    }

    private List<Long> getApprovedShop() {
        //先从缓存中查询shopList
        List<ShopVO> shopList = shopService.getShopList();
        if(CollUtils.isEmpty(shopList)){
            throw new BadRequestException("获取商铺列表失败");
        }
        List<Long> shopIds = shopList.stream()
                .filter(shop->shop.getApproved()==ApprovalStatus.APPROVED&&shop.getStatus()==ShopStatus.OPEN)
                .map(ShopVO::getId)
                .collect(Collectors.toList());
        if(CollUtils.isEmpty(shopIds)){
            throw new BadRequestException("没有审批通过的商铺,暂无商品展示");
        }
        return shopIds;
    }

    @Override
    public R<ProductDetailVO> detail(Long id) {
        //根据商品id查询商品
        EProduct product = getById(id);
        if(product==null){
            return R.error("商品不存在");
        }
        if(product.getStatus()==ProductStatus.OFF_SHELF){
            return R.error("商品已下架");
        }
        //封装productVO
        ProductDetailVO productDetailVO = BeanUtils.copyBean(product, ProductDetailVO.class);
        //查询缓存
        ShopVO shopVO = cacheService.hGetOrLoad(SHOP_ALL_KEY, product.getShopId().toString(), SHOP_TTL, ShopVO.class, () -> {
            EShop shop = shopService.getById(product.getShopId());
            return BeanUtils.copyBean(shop, ShopVO.class);
        });
        if(shopVO==null){
            return R.error("商品所属商铺不存在或未审批下来");
        }
        if(shopVO.getStatus()==ShopStatus.CLOSED){
            return R.error("商品所属商铺已关闭");
        }
        //根据分类id查询分类名称
        ECategory categoryInfo = categoryService.getById(product.getCategoryId());
        productDetailVO.setCategoryName(categoryInfo.getName());
        productDetailVO.setShopName(shopVO.getName());
        //根据商铺id查询sku表，得到规格列表
        List<SkuVO> listByProductId = productSkuService.getListByProductId(id);
        productDetailVO.setSkus(listByProductId);
        return R.ok(productDetailVO);
    }

    @Override
    @Transactional
    public R<Void> create(ProductCreateDTO dto) {
        //获取当前商家id
        Long userId = UserContext.getUserId();
        //封装spu
        EProduct SPU = BeanUtils.copyProperties(dto, EProduct.class, "skus");
        List<SkuCreateDTO> skusList = dto.getSkus();
        if(CollUtils.isEmpty(skusList)){
            return R.error("商品规格不能为空");
        }
        //计算总库存
        int totalStock = skusList.stream().mapToInt(SkuCreateDTO::getStock).sum();
        SPU.setTotalStock(totalStock);
        //计算最低价格
        int minPrice = skusList.stream().mapToInt(SkuCreateDTO::getPrice).min().orElse(0);
        SPU.setMinPrice(minPrice);
        //通过userId查询商铺id
        EShop shop = shopService
                .lambdaQuery()
                .eq(EShop::getOwnerId, userId)
                .eq(EShop::getApproved, ApprovalStatus.APPROVED)
                .one();
        if(shop==null){
            return R.error("当前商户没有商铺或者商铺未被审批");
        }
        //有店铺则创建商品
        SPU.setShopId(shop.getId());
        boolean isCreated = save(SPU);
        if(!isCreated){
            return R.error("创建商品失败");
        }
        //创建商品成功，保存商品sku
        List<EProductSku> skuPoList = skusList.stream().map(sku -> {
            //封装sku实体
            EProductSku skuPo = BeanUtils.copyProperties(sku, EProductSku.class, "specs");
            skuPo.setProductId(SPU.getId());
            skuPo.setSkuCode(snowflake.nextIdStr());
            skuPo.setStatus(SkuStatus.ENABLED);
            //序列化规格
            List<SpecDTO> specs = sku.getSpecs();
            if(CollUtils.isEmpty(specs)){
                return skuPo;
            }
            Map<String, String> specsMap = specs.stream().collect(Collectors.toMap(SpecDTO::getName, SpecDTO::getValue));
            skuPo.setSpecs(JSONUtil.toJsonStr(specsMap));
            return skuPo;
        }).collect(Collectors.toList());
        boolean savedBatch = productSkuService.saveBatch(skuPoList);
        if(!savedBatch){
            return R.error("批量新增sku失败");
        }
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> update(Long id, ProductUpdateDTO dto) {
        // 1. 校验商品归属
        EProduct product = getById(id);
        if (product == null) {
            return R.error("商品不存在");
        }
        Long userId = UserContext.getUserId();
        EShop shop = shopService.lambdaQuery().eq(EShop::getOwnerId, userId).one();
        if (shop == null || !shop.getId().equals(product.getShopId())) {
            return R.error("无权操作此商品");
        }

        // 2. 更新 SPU 基本字段（只改有值的）
        if (StringUtils.isNotBlank(dto.getName())) product.setName(dto.getName());
        if (dto.getCategoryId() != null) product.setCategoryId(dto.getCategoryId());
        if (dto.getImage() != null) product.setImage(dto.getImage());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());

        // 3. SKU 全量替换
        List<SkuCreateDTO> incomingSkus = dto.getSkus();
        if (CollUtils.isEmpty(incomingSkus)) {
            updateById(product);
            return R.ok();
        }

        List<EProductSku> existingSkus = productSkuService.lambdaQuery()
                .eq(EProductSku::getProductId, id)
                .list();
        Map<Long, EProductSku> existingMap = existingSkus.stream()
                .collect(Collectors.toMap(EProductSku::getId, s -> s));

        Set<Long> incomingIds = incomingSkus.stream()
                .map(SkuCreateDTO::getId)
                .filter(skuId -> skuId != null && skuId > 0)
                .collect(Collectors.toSet());

        int totalStock = 0;
        int minPrice = Integer.MAX_VALUE;

        List<EProductSku> toUpdate = new ArrayList<>();
        List<EProductSku> toInsert = new ArrayList<>();
        List<Long> toDelete = new ArrayList<>();

        for (SkuCreateDTO skuDto : incomingSkus) {
            if (skuDto.getId() != null && skuDto.getId() > 0 && existingMap.containsKey(skuDto.getId())) {
                EProductSku existing = existingMap.get(skuDto.getId());
                if (hasChanges(existing, skuDto)) {
                    applyChanges(existing, skuDto);
                    toUpdate.add(existing);
                }
            } else {
                toInsert.add(buildSku(skuDto, id));
            }
            totalStock += skuDto.getStock();
            if (skuDto.getPrice() < minPrice) minPrice = skuDto.getPrice();
        }

        for (Long existingId : existingMap.keySet()) {
            if (!incomingIds.contains(existingId)) {
                toDelete.add(existingId);
            }
        }

        if (!CollUtils.isEmpty(toInsert)) productSkuService.saveBatch(toInsert);
        if (!CollUtils.isEmpty(toUpdate)) productSkuService.updateBatchById(toUpdate);
        if (!CollUtils.isEmpty(toDelete)) productSkuService.removeByIds(toDelete);

        // 5. 更新 SPU 汇总
        product.setTotalStock(totalStock);
        product.setMinPrice(minPrice == Integer.MAX_VALUE ? 0 : minPrice);
        updateById(product);

        return R.ok();
    }

    private boolean hasChanges(EProductSku db, SkuCreateDTO dto) {
        return !Objects.equals(db.getPrice(), dto.getPrice())
            || !Objects.equals(db.getStock(), dto.getStock())
            || !Objects.equals(db.getSkuCode(), dto.getSkuCode())
            || !Objects.equals(db.getImage(), dto.getImage())
            || !Objects.equals(db.getSpecs(), specsToJson(dto.getSpecs()));
    }

    private void applyChanges(EProductSku sku, SkuCreateDTO dto) {
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        if (dto.getSkuCode() != null) sku.setSkuCode(dto.getSkuCode());
        if (dto.getImage() != null) sku.setImage(dto.getImage());
        if (dto.getSpecs() != null) sku.setSpecs(specsToJson(dto.getSpecs()));
    }

    private String specsToJson(List<SpecDTO> specs) {
        if (CollUtils.isEmpty(specs)) return "{}";
        Map<String, String> map = specs.stream()
                .collect(Collectors.toMap(SpecDTO::getName, SpecDTO::getValue));
        return JSONUtil.toJsonStr(map);
    }

    private EProductSku buildSku(SkuCreateDTO dto, Long productId) {
        EProductSku sku = BeanUtils.copyProperties(dto, EProductSku.class, "specs");
        sku.setProductId(productId);
        if (StringUtils.isBlank(dto.getSkuCode())) {
            sku.setSkuCode(snowflake.nextIdStr());
        }
        sku.setSpecs(specsToJson(dto.getSpecs()));
        sku.setStatus(SkuStatus.ENABLED);
        return sku;
    }

    /**
     * 商家上下架商品
     * @param id
     * @param status
     * @return
     */
    @Override
    public R<Void> updateStatus(Long id, ProductStatus status) {
        //获取当前商家id
        Long userId = UserContext.getUserId();
        if(userId==null){
            throw new BadRequestException("请先登录");
        }
        Long shopId = cacheService.hGetOrLoad(USER_SHOP_RELATED_KEY, userId.toString(), null, Long.class, () -> {
            EShop shop = shopService.lambdaQuery().eq(EShop::getOwnerId, userId).one();
            return shop != null ? shop.getId() : null;
        });
        if(shopId==null){
            throw new BadRequestException("你还没有商铺，无权操作此商品");
        }
        boolean update = lambdaUpdate()
                .eq(EProduct::getShopId, shopId)
                .eq(EProduct::getId, id)
                .set(EProduct::getStatus, status)
                .update();
        if(!update){
            return R.error("状态变更失败");
        }
            return R.ok();
    }


    /**
     * 商家获取自己商品的列表
     * @param query
     * @return
     */
    @Override
    public R<PageDTO<ProductVO>> myProducts(PageQuery query) {
        //获取商家id
        Long userId = UserContext.getUserId();
        //根据商家id获取商铺id
        EShop shopInfo = shopService.lambdaQuery().eq(EShop::getOwnerId, userId).one();
        if(shopInfo==null){
            throw new BadRequestException("当前用户没有商铺");
        }
        //根据商铺id查询商品列表
        Page<EProduct> page = lambdaQuery()
                .eq(EProduct::getShopId, shopInfo.getId())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EProduct> goods = page.getRecords();
        if(CollUtils.isEmpty(goods)){
            return R.ok(PageDTO.empty(page));
        }
        //根据分类id查询分类名称
        Set<Long> cataIds = goods.stream().map(EProduct::getCategoryId).collect(Collectors.toSet());
        List<ECategory> cataList = categoryService.lambdaQuery().in(ECategory::getId, cataIds).list();
        if(CollUtils.isEmpty(cataList)){
            //业务异常
            log.error("商品分类不存在,请检查数据库");
            return R.ok(PageDTO.empty(page));
        }
        Map<Long, String> cataMap = cataList.stream().collect(Collectors.toMap(ECategory::getId, ECategory::getName));
        // 批量查询所有商品的SKU
        List<Long> productIds = goods.stream().map(EProduct::getId).collect(Collectors.toList());
        List<SkuVO> allSkus = productSkuService.getListByProductIds(productIds);
        Map<Long, List<SkuVO>> skuMap = allSkus.stream().collect(Collectors.groupingBy(SkuVO::getProductId));
        List<ProductVO> voList = goods.stream()
                .map(good -> {
                    ProductVO productVO = BeanUtils.copyBean(good, ProductVO.class);
                    //设置商店名
                    productVO.setShopName(shopInfo.getName());
                    //设置分类名
                    String categoryName = cataMap.get(good.getCategoryId());
                    if (categoryName == null) {
                        log.error("商品分类不存在,请检查数据库");
                        return null;
                    }
                    productVO.setCategoryName(categoryName);
                    // 设置SKU列表
                    productVO.setSkus(skuMap.getOrDefault(good.getId(), CollUtils.emptyList()));
                    return productVO;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return R.ok(PageDTO.of(page, voList));
    }

    @Override
    public R<List<ProductVO>> getDetailsByIds(Set<Long> ids) {
        List<EProduct> eProducts = listByIds(ids);
        List<ProductVO> voList = BeanUtils.copyList(eProducts, ProductVO.class);
        //根据分类id查询分类名称（优先缓存，缓存未命中再找数据库）
        List<CategoryVO> categoryVOList = cacheService.getOrLoadList(CATEGORY_ALL_KEY, CATEGORY_TTL, CategoryVO.class, () -> {
            List<ECategory> list = categoryService.list();
            return BeanUtils.copyList(list, CategoryVO.class);
        });
        Map<Long, String> cateMap = categoryVOList.stream()
                .collect(Collectors.toMap(CategoryVO::getId, CategoryVO::getName));
        //根据商品id查询商店名称
        Set<String> shopIds = eProducts.stream().map(e->e.getShopId().toString()).collect(Collectors.toSet());
        Map<String, ShopVO> shopVoMap = cacheService.hMGetOrLoad(SHOP_ALL_KEY, shopIds, SHOP_TTL, ShopVO.class, (missed) -> {
            Set<Long> Ids = missed.stream().map(Long::valueOf).collect(Collectors.toSet());
            List<EShop> shops = shopService.listByIds(Ids);
            if (CollUtils.isEmpty(shops)) {
                return Collections.emptyMap();
            }
            return BeanUtils.copyList(shops, ShopVO.class)
                    .stream()
                    .collect(Collectors.toMap(shop -> shop.getId().toString(), shop -> shop));
        });
        voList.forEach(vo -> {
            vo.setCategoryName(cateMap.get(vo.getCategoryId()));
            ShopVO shop = shopVoMap.get(vo.getShopId().toString());
            vo.setShopName(shop != null ? shop.getName() : null);
        });
        //根据商品id批量查询sku列表
        List<Long> productIds = eProducts.stream().map(EProduct::getId).collect(Collectors.toList());
        List<SkuVO> allSkus = productSkuService.getListByProductIds(productIds);
        Map<Long, List<SkuVO>> skuMap = allSkus.stream()
                .collect(Collectors.groupingBy(SkuVO::getProductId));
        voList.forEach(vo -> vo.setSkus(skuMap.getOrDefault(vo.getId(), Collections.emptyList())));
        return R.ok(voList);
    }

}
