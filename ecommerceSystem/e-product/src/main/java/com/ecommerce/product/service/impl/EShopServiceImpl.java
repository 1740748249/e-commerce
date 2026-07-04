package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.UserClient;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.api.enums.UserRole;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.DbException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.product.domain.dto.ShopApplyDTO;
import com.ecommerce.product.domain.po.ECategory;
import com.ecommerce.product.domain.po.EProduct;
import com.ecommerce.product.domain.po.EShop;
import com.ecommerce.product.domain.vo.CategoryVO;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.product.domain.vo.ShopApplicationVO;
import com.ecommerce.product.domain.vo.ShopVO;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.mapper.EProductMapper;
import com.ecommerce.product.mapper.EShopMapper;
import com.ecommerce.product.service.IECategoryService;
import com.ecommerce.product.service.IEShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecommerce.product.constants.CacheConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EShopServiceImpl extends ServiceImpl<EShopMapper, EShop> implements IEShopService {
    private final CacheService cacheService;
    private final UserClient userClient;
    private final EProductMapper productMapper;
    private final IECategoryService categoryService;

    @Override
    public R<Void> apply(ShopApplyDTO dto) {
        Long userId = UserContext.getUserId();
        EShop eShop = BeanUtils.copyBean(dto, EShop.class);
        eShop.setOwnerId(userId);
        eShop.setApproved(ApprovalStatus.PENDING);
        boolean success = save(eShop);
        if (!success) {
            return R.error("申请失败");
        }
        //只要申请开店就把旧缓存删了
        cacheService.delete(SHOP_ALL_KEY);
        return R.ok();
    }

    @Override
    public R<ShopVO> getMyShop() {
        Long userId = UserContext.getUserId();
        EShop shopInfo = lambdaQuery().eq(EShop::getOwnerId, userId).one();
        if (shopInfo == null) {
            return null;
        }
        ShopVO shopVO = BeanUtils.copyBean(shopInfo, ShopVO.class);
        return R.ok(shopVO);
    }

    @Override
    public List<ShopVO> getShopList() {
        Map<String, ShopVO> map = cacheService.hGetAllOrLoad(SHOP_ALL_KEY, SHOP_TTL, ShopVO.class, () -> {
            //查询数据库
            List<EShop> shops = list();
            List<ShopVO> voList = BeanUtils.copyList(shops, ShopVO.class);
            if (CollUtils.isEmpty(voList)) {
                return CollUtils.emptyMap();
            }
            return voList.stream()
                    .collect(Collectors.toMap(shopVo -> shopVo.getId().toString(), shopVo -> shopVo));
        });
        if(map.isEmpty()){
            return CollUtils.emptyList();
        }
        ArrayList<ShopVO> voList = new ArrayList<>(map.size());
        map.forEach((key,value)->{
            if(value!=null){
                voList.add(value);
            }
        });
        return voList;
    }

    @Override
    public R<PageDTO<ShopApplicationVO>> getPendingShops(PageQuery query) {
        // TODO: 业务逻辑待实现（需通过 Feign 调用 user-service 获取申请人用户名/手机号）
        Page<EShop> result = lambdaQuery()
                .eq(EShop::getApproved, ApprovalStatus.PENDING)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EShop> pendingShops = result.getRecords();
        if(CollUtils.isEmpty(pendingShops)){
           return R.ok(PageDTO.of(result, CollUtils.emptyList()));
        }
        List<Long> ownerIds = pendingShops.stream()
                .map(EShop::getOwnerId)
                .collect(Collectors.toList());
        List<ShopApplicationVO> voList = BeanUtils.copyList(pendingShops, ShopApplicationVO.class,(source,target)->{
            target.setShopName(source.getName());
            target.setShopId(source.getId());
        });
        //根据userID远程feign查询user微服务
        List<UserBriefDTO> data = userClient.getUsersByIds(ownerIds).getData();
        if(CollUtils.isEmpty(data)){
            //查不到任何数据信息，证明商户不存在
            return R.error("用户可能已被注销");
        }
        Map<Long, UserBriefDTO> userMap = data
                .stream()
                .collect(Collectors.toMap(UserBriefDTO::getId, item -> item,(a,b)->a));
        for (ShopApplicationVO shopApplicationVO : voList) {
            Long ownerId = shopApplicationVO.getOwnerId();
            UserBriefDTO dto = userMap.get(ownerId);
            if(dto==null){
                continue;
            }
            shopApplicationVO.setUserName(dto.getUsername());
            shopApplicationVO.setUserPhone(dto.getPhone());
        }
        return R.ok(PageDTO.of(result,voList));
    }

    @Override
    public R<PageDTO<ProductVO>> shopProducts(Long shopId, PageQuery query, String sort) {
        //构建分页查询参数带排序条件
        Page<EProduct> mpage = new Page<>(query.getPage(), query.getSize());
        if (sort == null) {
            mpage.addOrder(OrderItem.desc("create_time"));
        } else if (sort.equals("price_asc")) {
            mpage.addOrder(OrderItem.asc("min_price"));
        } else if (sort.equals("price_desc")) {
            mpage.addOrder(OrderItem.desc("min_price"));
        } else {
            mpage.addOrder(OrderItem.desc("sales"));
        }
        //分页查询该店铺已上架商品
        Page<EProduct> page = productMapper.selectPage(mpage,
                new LambdaQueryWrapper<EProduct>()
                        .eq(EProduct::getShopId, shopId)
                        .eq(EProduct::getStatus, ProductStatus.ON_SHELF));
        List<EProduct> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return R.ok(PageDTO.empty(page));
        }
        //查询缓存获取分类名
        List<CategoryVO> categoryVOList = cacheService.getOrLoadList(CATEGORY_ALL_KEY, CATEGORY_TTL, CategoryVO.class, () -> {
            List<ECategory> cataList = categoryService.lambdaQuery().list();
            return BeanUtils.copyList(cataList, CategoryVO.class);
        });
        //查询缓存获取店铺信息
        ShopVO shopVO = cacheService.hGetOrLoad(SHOP_ALL_KEY, shopId.toString(), SHOP_TTL, ShopVO.class, () -> {
            EShop shop = getById(shopId);
            return BeanUtils.copyBean(shop, ShopVO.class);
        });
        if (shopVO == null || shopVO.getName() == null) {
            log.error("商店不存在,请检查数据库");
            throw new BadRequestException("商店不存在,请检查数据库");
        }
        //转换VO
        List<ProductVO> voList = records.stream().map(pro -> {
            ProductVO vo = BeanUtils.copyBean(pro, ProductVO.class);
            String cateName = categoryVOList.stream()
                    .filter(cate -> cate.getId().equals(pro.getCategoryId()))
                    .map(CategoryVO::getName)
                    .findFirst()
                    .orElse(null);
            if (cateName == null) {
                log.error("商品分类不存在,请检查数据库");
                throw new BadRequestException("商品分类不存在,请检查数据库");
            }
            vo.setCategoryName(cateName);
            vo.setShopName(shopVO.getName());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(PageDTO.of(page, voList));
    }

    @Override
    public Long getPendingShopCount() {
        return lambdaQuery()
                .eq(EShop::getApproved, ApprovalStatus.PENDING)
                .count();
    }

    @Override
    @Transactional
    public R<Void> approveShop(Long shopId, Boolean approved) {
        //根据shopId查询用户信息
        EShop shop = lambdaQuery()
                .eq(EShop::getId, shopId)
                .eq(EShop::getApproved, ApprovalStatus.PENDING)
                .one();
        if(shop==null){
            throw new BadRequestException("目标商铺不可操作");
        }
        Long userId = shop.getOwnerId();
        boolean update = lambdaUpdate()
                .eq(EShop::getId, shopId)
                .eq(EShop::getApproved, ApprovalStatus.PENDING)
                .set(approved, EShop::getApproved, ApprovalStatus.APPROVED)
                .set(!approved, EShop::getApproved, ApprovalStatus.REJECTED)
                .update();
        if(!update){
            throw new DbException("操作失败");
        }
        if (approved) {
            userClient.updateUserRole(userId, UserRole.VENDOR);
            cacheService.hSet(USER_SHOP_RELATED_KEY, userId.toString(), shopId);
        }
        cacheService.delete(SHOP_ALL_KEY);
        return R.ok();
    }
}
