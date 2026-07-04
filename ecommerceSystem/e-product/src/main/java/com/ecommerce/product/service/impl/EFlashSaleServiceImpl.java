package com.ecommerce.product.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.lang.Snowflake;
import com.ecommerce.api.client.UserClient;
import com.ecommerce.api.dto.AddressDTO;
import cn.hutool.core.util.IdUtil;
import com.ecommerce.api.message.FlashSaleOrderMessage;
import com.ecommerce.api.message.FlashStockSyncMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.product.domain.dto.ApprovalDTO;
import com.ecommerce.product.domain.dto.FlashSaleCreateDTO;
import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.domain.po.EFlashSession;
import com.ecommerce.product.domain.po.EProduct;
import com.ecommerce.product.domain.po.EShop;
import com.ecommerce.product.domain.vo.FlashSaleItemVO;
import com.ecommerce.product.domain.vo.FlashSaleOrderVO;
import com.ecommerce.product.domain.vo.FlashSaleVO;
import com.ecommerce.product.domain.vo.ShopVO;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.FlashSaleOrderStatus;
import com.ecommerce.product.mapper.EFlashSaleMapper;
import com.ecommerce.product.service.IEFlashSaleService;
import com.ecommerce.product.service.IEFlashSessionService;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import com.ecommerce.product.service.IEProductService;
import com.ecommerce.product.service.IEShopService;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.product.domain.query.FlashSaleApplicationQuery;
import com.ecommerce.product.domain.vo.FlashSaleApplicationVO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_SYNC_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_FLASH_CREATE_KEY;
import static com.ecommerce.product.constants.CacheConstants.*;

@Service
@RequiredArgsConstructor
public class EFlashSaleServiceImpl extends ServiceImpl<EFlashSaleMapper, EFlashSale> implements IEFlashSaleService {

    private final IEFlashSessionService flashSessionService;
    private final IEProductService productService;
    private final IEShopService shopService;
    private final CacheService cacheService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> flashDeductScript;
    private final DefaultRedisScript<Long> flashRollbackScript;
    private final DefaultRedisScript<Long> flashCompensatedRollbackScript;
    private final RabbitMqHelper rabbitMqHelper;
    private final IEFlashSaleOrderService flashSaleOrderService;
    private final UserClient userClient;
    private final Snowflake snowflake;

    // ==================== 用户端 ====================

    @Override
    public R<List<FlashSaleVO>> listFlashSales(Long sessionId) {
        List<EFlashSession> sessions;
        // 指定场次：只查一个；未指定：查所有进行中的场次
        if (sessionId != null) {
            EFlashSession s = flashSessionService.lambdaQuery()
                    .eq(EFlashSession::getId, sessionId)
                    .one();
            sessions = s != null ? List.of(s) : Collections.emptyList();
        } else {
            LocalDateTime now = LocalDateTime.now();
            sessions = flashSessionService.lambdaQuery()
                    .le(EFlashSession::getStartTime, now)
                    .ge(EFlashSession::getEndTime, now)
                    .orderByAsc(EFlashSession::getStartTime)
                    .list();
        }
        if (CollUtils.isEmpty(sessions)) {
            return R.ok(Collections.emptyList());
        }

        // 批量查所有场次下的已审批秒杀商品
        Set<Long> sessionIds = sessions.stream().map(EFlashSession::getId).collect(Collectors.toSet());
        List<EFlashSale> allItems = lambdaQuery()
                .in(EFlashSale::getSessionId, sessionIds)
                .eq(EFlashSale::getApprovalStatus, ApprovalStatus.APPROVED)
                .list();

        // 批量查商品信息
        Set<Long> productIds = allItems.stream().map(EFlashSale::getProductId).collect(Collectors.toSet());
        Map<Long, EProduct> proMap = CollUtils.isEmpty(productIds) ? Collections.emptyMap()
                : productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(EProduct::getId, p -> p));

        // 按场次分组，组装 VO
        Map<Long, List<EFlashSale>> grouped = allItems.stream()
                .collect(Collectors.groupingBy(EFlashSale::getSessionId));
        List<FlashSaleVO> result = sessions.stream().map(session -> {
            FlashSaleVO vo = BeanUtils.copyBean(session, FlashSaleVO.class);
            List<EFlashSale> items = grouped.getOrDefault(session.getId(), Collections.emptyList());
            List<FlashSaleItemVO> itemVOs = items.stream().map(item -> {
                FlashSaleItemVO iv = BeanUtils.copyBean(item, FlashSaleItemVO.class);
                EProduct product = proMap.get(item.getProductId());
                if (product == null) return null;
                iv.setProductImage(product.getImage());
                iv.setProductName(product.getName());
                iv.setOriginalPrice(product.getMinPrice());
                // 店铺名从缓存取
                ShopVO shopVO = cacheService.hGetOrLoad(SHOP_ALL_KEY, item.getShopId().toString(),
                        SHOP_TTL, ShopVO.class, () -> {
                            EShop shop = shopService.getById(item.getShopId());
                            return shop != null ? BeanUtils.copyBean(shop, ShopVO.class) : null;
                        });
                if (shopVO == null) return null;
                iv.setShopName(shopVO.getName());
                int stock = iv.getStock() != null ? iv.getStock() : 0;
                int sold = iv.getSold() != null ? iv.getSold() : 0;
                iv.setProgress(stock > 0 ? sold * 100 / stock : 0);
                return iv;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            vo.setItems(itemVOs);
            return vo;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    // ==================== 商家端 ====================

    @Override
    public R<Void> create(FlashSaleCreateDTO dto) {
        // 1. 获取当前商家店铺ID
        Long shopId = UserContext.getShopId();
        if (shopId == null) {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                throw new BizIllegalException("请先登录");
            }
            EShop shop = shopService.lambdaQuery()
                    .eq(EShop::getOwnerId, userId)
                    .one();
            if (shop == null) {
                throw new BizIllegalException("您还未开通店铺");
            }
            shopId = shop.getId();
            cacheService.hSet(USER_SHOP_RELATED_KEY, userId.toString(),
                    shopId);
        }
        // 2. 校验场次是否存在且未开始
        EFlashSession session = flashSessionService.getById(dto.getSessionId());
        if (session == null) {
            throw new BizIllegalException("秒杀场次不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(session.getStartTime())) {
            throw new BizIllegalException("秒杀场次已开始，无法报名");
        }
        // 3. 校验商品是否属于当前商家
        EProduct product = productService.getById(dto.getProductId());
        if (product == null) {
            throw new BizIllegalException("商品不存在");
        }
        if (!shopId.equals(product.getShopId())) {
            throw new BizIllegalException("商品不属于当前商家店铺");
        }
        // 4. 校验秒杀价低于原价
        if (dto.getFlashPrice() >= product.getMinPrice()) {
            throw new BizIllegalException("秒杀价必须低于商品原价");
        }
        // 5. 同一商品同一场次仅可报名一次
        Long count = lambdaQuery()
                .eq(EFlashSale::getSessionId, dto.getSessionId())
                .eq(EFlashSale::getProductId, dto.getProductId())
                .count();
        if (count > 0) {
            throw new BizIllegalException("该商品已报名此场次");
        }
        // 6. 报名成功，待审核
        EFlashSale flashSale = BeanUtils.copyBean(dto, EFlashSale.class);
        flashSale.setShopId(shopId);
        flashSale.setApprovalStatus(ApprovalStatus.PENDING);
        flashSale.setSold(0);
        flashSale.setPerUserLimit(dto.getPerUserLimit() != null ? dto.getPerUserLimit() : 1);
        save(flashSale);
        return R.ok();
    }

    @Override
    public R<FlashSaleOrderVO> order(Long flashSaleId, Integer quantity, Long addressId) {
        // 1. 获取当前用户
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }

        // 2. 查询秒杀活动（缓存优先，减少 DB 查询）
        EFlashSale flashSale = loadFlashSale(flashSaleId);
        if (flashSale == null) {
            throw new BizIllegalException("秒杀活动不存在");
        }
        if (flashSale.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BizIllegalException("秒杀活动未开放");
        }

        // 3. 校验场次时间（缓存优先）
        EFlashSession session = cacheService.hGetOrLoad(SESSION_ALL_KEY,
                flashSale.getSessionId().toString(), SESSION_TTL, EFlashSession.class,
                () -> flashSessionService.getById(flashSale.getSessionId()));
        LocalDateTime now = LocalDateTime.now();

        // 3a. 刷新秒杀活动缓存 TTL（对齐场次结束时间，取代 loadFlashSale 中的固定 10 分钟）
        redisTemplate.opsForValue().set(FLASH_SALE_PREFIX + flashSaleId,
                JSONUtil.toJsonStr(flashSale),
                Duration.ofSeconds(ChronoUnit.SECONDS.between(now, session.getEndTime()) + 3600));
        if (now.isBefore(session.getStartTime())) {
            throw new BizIllegalException("秒杀尚未开始");
        }
        if (now.isAfter(session.getEndTime())) {
            throw new BizIllegalException("秒杀已结束");
        }

        // 4. 购买数量校验：默认 1，上限取 min(每人限购, 系统上限 5)
        int perUserLimit = flashSale.getPerUserLimit() != null ? flashSale.getPerUserLimit() : 1;
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        int maxQuantity = Math.min(perUserLimit, 5);
        if (quantity > maxQuantity) {
            throw new BizIllegalException("购买数量超出限制");
        }

        // 4a. 提前校验商品是否存在
        EProduct product = productService.getById(flashSale.getProductId());
        if (product == null) {
            throw new BizIllegalException("商品已下架");
        }

        // 4b. 提前校验收货地址，同时校验地址归属
        AddressDTO address = userClient.getAddressById(addressId).getData();
        if (address == null) {
            throw new BizIllegalException("收货地址不存在");
        }

        // 5. 执行 Lua 原子扣减库存 + 校验限购
        String stockKey = FLASH_STOCK_PREFIX + flashSaleId;
        String userKey = FLASH_USER_PREFIX + flashSaleId;
        long ttlSeconds = ChronoUnit.SECONDS.between(now, session.getEndTime()) + 3600;

        // 5a. 库存 key 不存在时从 DB 初始化（预热遗漏：如审核通过晚于预热窗口）
        //     使用 setIfAbsent 避免 TOCTOU 竞态导致超卖
        redisTemplate.opsForValue().setIfAbsent(stockKey,
                String.valueOf(flashSale.getStock()),
                Duration.ofSeconds(ttlSeconds));

        Long result = redisTemplate.execute(flashDeductScript,
                List.of(stockKey, userKey),
                String.valueOf(userId),
                String.valueOf(quantity),
                String.valueOf(perUserLimit),
                String.valueOf(ttlSeconds));

        if (result == null || result == 0) {
            throw new BizIllegalException("库存不足");
        }
        if (result == -1) {
            throw new BizIllegalException("超出限购数量");
        }

        // 6. 预生成正式订单号，写入秒杀订单记录（失败时回滚 Redis 库存）
        long orderNo = snowflake.nextId();
        EFlashSaleOrder order = new EFlashSaleOrder();
        order.setUserId(userId);
        order.setFlashSaleId(flashSaleId);
        order.setOrderNo(orderNo);
        order.setProductId(flashSale.getProductId());
        order.setQuantity(quantity);
        order.setPrice(flashSale.getFlashPrice());
        order.setStatus(FlashSaleOrderStatus.PENDING_PAYMENT);
        try {
            flashSaleOrderService.save(order);
        } catch (Exception e) {
            log.error("保存秒杀订单失败，回滚Redis库存 flashSaleId=" + flashSaleId
                    + " userId=" + userId + " quantity=" + quantity, e);
            redisTemplate.execute(flashRollbackScript,
                    List.of(stockKey, userKey),
                    String.valueOf(userId), String.valueOf(quantity));
            throw new BizIllegalException("系统繁忙，请稍后重试");
        }

        // 6a. 缓存秒杀订单到 Redis，供 result() 快速查询
        String orderCacheKey = FLASH_ORDER_PREFIX + flashSaleId + ":" + userId;
        redisTemplate.opsForValue().set(orderCacheKey,
                JSONUtil.toJsonStr(order),
                Duration.ofSeconds(ttlSeconds));

        // 7. 从缓存获取最低价 SKU（预热时已缓存）
        String skuKey = FLASH_SKU_PREFIX + flashSaleId;
        String skuIdStr = redisTemplate.opsForValue().get(skuKey);
        Long skuId = skuIdStr != null ? Long.valueOf(skuIdStr) : null;

        // 7a. 拼接完整地址
        String fullAddr = String.join(" ", address.getProvince(), address.getCity(),
                address.getDistrict(), address.getDetail());

        // 8. MQ 发送（同步），失败时补偿回滚 Redis + 标记 DB
        FlashSaleOrderMessage msg = FlashSaleOrderMessage.builder()
                .flashSaleOrderId(order.getId())
                .orderNo(orderNo)
                .flashSaleId(flashSaleId)
                .userId(userId)
                .productId(flashSale.getProductId())
                .skuId(skuId)
                .shopId(flashSale.getShopId())
                .addressId(addressId)
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .receiverAddr(fullAddr)
                .productName(product.getName())
                .productImage(product.getImage())
                .quantity(quantity)
                .price(flashSale.getFlashPrice())
                .build();
        try {
            rabbitMqHelper.send(ORDER_EXCHANGE, ORDER_FLASH_CREATE_KEY, msg);
        } catch (Exception e) {
            log.error("MQ 发送失败，补偿回滚: flashSaleOrderId=" + order.getId(), e);
            // 先更新 DB（乐观锁防支付并发），再回补 Redis
            flashSaleOrderService.lambdaUpdate()
                    .set(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PAYMENT_TIMEOUT)
                    .eq(EFlashSaleOrder::getId, order.getId())
                    .eq(EFlashSaleOrder::getStatus, FlashSaleOrderStatus.PENDING_PAYMENT)
                    .update();
            redisTemplate.execute(flashCompensatedRollbackScript,
                    List.of(stockKey, userKey,
                            "flash:compensated:" + order.getId()),
                    String.valueOf(userId), String.valueOf(quantity),
                    String.valueOf(Duration.ofHours(24).getSeconds()));
            redisTemplate.delete(orderCacheKey);
            throw new BizIllegalException("系统繁忙，请稍后重试");
        }
        // 异步同步 DB 库存，非关键路径，置于 try-catch 外避免因 sendAsync 异常错误回滚
        rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_STOCK_SYNC_KEY,
                FlashStockSyncMessage.builder()
                        .messageId(IdUtil.fastSimpleUUID())
                        .flashSaleId(flashSaleId)
                        .quantity(quantity)
                        .build());

        // 9. 返回 VO
        FlashSaleOrderVO vo = BeanUtils.copyBean(order, FlashSaleOrderVO.class);
        vo.setOrderNo(orderNo);
        vo.setStatusText(order.getStatus().getDesc());
        return R.ok(vo);
    }

    private EFlashSale loadFlashSale(Long flashSaleId) {
        String key = FLASH_SALE_PREFIX + flashSaleId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return JSONUtil.toBean(json, EFlashSale.class);
        }
        // 缓存未命中，从 DB 加载并回填缓存（预热正常时极少走到这里）
        EFlashSale flashSale = getById(flashSaleId);
        if (flashSale != null) {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(flashSale),
                    Duration.ofMinutes(10));
        }
        return flashSale;
    }

    @Override
    public R<FlashSaleOrderVO> result(Long flashSaleId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizIllegalException("请先登录");
        }

        // 优先读缓存（下单时已写入），未命中再查 DB
        String orderCacheKey = FLASH_ORDER_PREFIX + flashSaleId + ":" + userId;
        String cachedJson = redisTemplate.opsForValue().get(orderCacheKey);
        EFlashSaleOrder order = null;
        if (cachedJson != null) {
            order = JSONUtil.toBean(cachedJson, EFlashSaleOrder.class);
        } else {
            order = flashSaleOrderService.lambdaQuery()
                    .eq(EFlashSaleOrder::getFlashSaleId, flashSaleId)
                    .eq(EFlashSaleOrder::getUserId, userId)
                    .orderByDesc(EFlashSaleOrder::getCreateTime)
                    .last("LIMIT 1")
                    .one();
        }

        if (order == null) {
            // 未抢到，检查是否还有库存
            String stockKey = FLASH_STOCK_PREFIX + flashSaleId;
            String stockStr = redisTemplate.opsForValue().get(stockKey);
            if (stockStr != null && Integer.parseInt(stockStr) > 0) {
                return R.ok(null);
            }
            FlashSaleOrderVO vo = new FlashSaleOrderVO();
            vo.setStatus(0);
            vo.setStatusText("已售罄");
            return R.ok(vo);
        }

        FlashSaleOrderVO vo = BeanUtils.copyBean(order, FlashSaleOrderVO.class);
        vo.setStatusText(order.getStatus().getDesc());
        return R.ok(vo);
    }

    @Override
    public R<PageDTO<FlashSaleApplicationVO>> listApplications(FlashSaleApplicationQuery query) {
        // 1. 构建查询条件
        Page<EFlashSale> result = lambdaQuery()
                .eq(query.getSessionId() != null, EFlashSale::getSessionId, query.getSessionId())
                .eq(query.getApprovalStatus() != null, EFlashSale::getApprovalStatus, query.getApprovalStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EFlashSale> records = result.getRecords();
        if (CollUtils.isEmpty(records)) {
            return R.ok(PageDTO.empty(result));
        }
        // 2. 批量查询关联信息（场次和店铺走缓存，商品走DB）
        Set<Long> productIds = records.stream().map(EFlashSale::getProductId).collect(Collectors.toSet());
        Set<String> sessionIdStrs = records.stream()
                .map(fs -> fs.getSessionId().toString()).collect(Collectors.toSet());
        Set<String> shopIdStrs = records.stream()
                .map(fs -> fs.getShopId().toString()).collect(Collectors.toSet());

        Map<Long, EProduct> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(EProduct::getId, Function.identity()));
        Map<String, EFlashSession> sessionMap = cacheService.hMGetOrLoad(
                SESSION_ALL_KEY, sessionIdStrs, SESSION_TTL, EFlashSession.class,
                missed -> {
                    Set<Long> ids = missed.stream().map(Long::valueOf).collect(Collectors.toSet());
                    Map<Long, EFlashSession> existing = flashSessionService.listByIds(ids).stream()
                            .collect(Collectors.toMap(EFlashSession::getId, Function.identity()));
                    Map<String, EFlashSession> map = new HashMap<>();
                    for (String id : missed) {
                        map.put(id, existing.get(Long.valueOf(id)));
                    }
                    return map;
                });
        Map<String, ShopVO> shopMap = cacheService.hMGetOrLoad(
                SHOP_ALL_KEY, shopIdStrs, SHOP_TTL, ShopVO.class,
                missed -> {
                    Set<Long> ids = missed.stream().map(Long::valueOf).collect(Collectors.toSet());
                    Map<Long, ShopVO> existing = shopService.listByIds(ids).stream()
                            .map(s -> BeanUtils.copyBean(s, ShopVO.class))
                            .collect(Collectors.toMap(ShopVO::getId, Function.identity()));
                    Map<String, ShopVO> map = new HashMap<>();
                    for (String id : missed) {
                        map.put(id, existing.get(Long.valueOf(id)));
                    }
                    return map;
                });
        // 3. 组装VO
        List<FlashSaleApplicationVO> voList = records.stream().map(fs -> {
            FlashSaleApplicationVO vo = BeanUtils.copyBean(fs, FlashSaleApplicationVO.class);
            EFlashSession session = sessionMap.get(fs.getSessionId().toString());
            if (session != null) {
                vo.setSessionName(session.getName());
            }
            EProduct product = productMap.get(fs.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getImage());
                vo.setOriginalPrice(product.getMinPrice());
            }
            ShopVO shopVO = shopMap.get(fs.getShopId().toString());
            if (shopVO != null) {
                vo.setShopName(shopVO.getName());
            }
            vo.setApprovalStatusText(fs.getApprovalStatus().getDesc());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(PageDTO.of(result, voList));
    }

    @Override
    public R<Void> approve(Long id, ApprovalDTO dto) {
        EFlashSale flashSale = getById(id);
        if (flashSale == null) {
            throw new BizIllegalException("秒杀报名不存在");
        }
        if (flashSale.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BizIllegalException("仅可审核待审核状态的报名");
        }
        if (dto.getApproved()) {
            flashSale.setApprovalStatus(ApprovalStatus.APPROVED);
        } else {
            if (dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
                throw new BizIllegalException("拒绝时必须填写拒绝原因");
            }
            flashSale.setApprovalStatus(ApprovalStatus.REJECTED);
            flashSale.setRejectReason(dto.getRejectReason());
        }
        updateById(flashSale);
        return R.ok();
    }

    @Override
    public R<PageDTO<FlashSaleApplicationVO>> myApplications(ApprovalStatus approvalStatus, PageQuery query) {
        // 1. 获取当前商家店铺ID（优先 UserContext，其次缓存+DB）
        Long shopId = UserContext.getShopId();
        if (shopId == null) {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                throw new BizIllegalException("请先登录");
            }
            shopId = cacheService.hGetOrLoad(USER_SHOP_RELATED_KEY, userId.toString(),
                    null, Long.class, () -> {
                EShop shop = shopService.lambdaQuery()
                        .eq(EShop::getOwnerId, userId)
                        .one();
                return shop != null ? shop.getId() : null;
            });
        }
        if (shopId == null) {
            throw new BizIllegalException("您还未开通店铺");
        }
        // 2. 查询当前店铺的报名记录
        Page<EFlashSale> result = lambdaQuery()
                .eq(EFlashSale::getShopId, shopId)
                .eq(approvalStatus != null, EFlashSale::getApprovalStatus, approvalStatus)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EFlashSale> records = result.getRecords();
        if (CollUtils.isEmpty(records)) {
            return R.ok(PageDTO.empty(result));
        }
        // 3. 批量查询商品信息
        Set<Long> productIds = records.stream().map(EFlashSale::getProductId).collect(Collectors.toSet());
        Map<Long, EProduct> productMap = productService.listByIds(productIds).stream()
                .collect(Collectors.toMap(EProduct::getId, Function.identity()));
        // 4. 批量查询场次信息（走缓存）
        Set<String> sessionIdStrs = records.stream()
                .map(fs -> fs.getSessionId().toString()).collect(Collectors.toSet());
        Map<String, EFlashSession> sessionMap = cacheService.hMGetOrLoad(
                SESSION_ALL_KEY, sessionIdStrs, SESSION_TTL, EFlashSession.class,
                missed -> {
                    Set<Long> ids = missed.stream().map(Long::valueOf).collect(Collectors.toSet());
                    Map<Long, EFlashSession> existing = flashSessionService.listByIds(ids).stream()
                            .collect(Collectors.toMap(EFlashSession::getId, Function.identity()));
                    Map<String, EFlashSession> map = new HashMap<>();
                    for (String id : missed) {
                        map.put(id, existing.get(Long.valueOf(id)));
                    }
                    return map;
                });
        // 5. 组装VO
        List<FlashSaleApplicationVO> voList = records.stream().map(fs -> {
            FlashSaleApplicationVO vo = BeanUtils.copyBean(fs, FlashSaleApplicationVO.class);
            EFlashSession session = sessionMap.get(fs.getSessionId().toString());
            if (session != null) {
                vo.setSessionName(session.getName());
            }
            EProduct product = productMap.get(fs.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getImage());
                vo.setOriginalPrice(product.getMinPrice());
            }
            vo.setApprovalStatusText(fs.getApprovalStatus().getDesc());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(PageDTO.of(result, voList));
    }
}
