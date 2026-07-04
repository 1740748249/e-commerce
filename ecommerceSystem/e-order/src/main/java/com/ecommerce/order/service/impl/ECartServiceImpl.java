package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.api.dto.SkuVO;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.order.domain.dto.CartAddDTO;
import com.ecommerce.order.domain.dto.CartSyncMessage;
import com.ecommerce.order.domain.po.ECart;
import com.ecommerce.order.domain.vo.CartVO;
import com.ecommerce.order.mapper.ECartMapper;
import com.ecommerce.order.service.IECartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.DELAY_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.CART_SYNC_KEY;
import static com.ecommerce.order.constants.RedisConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ECartServiceImpl extends ServiceImpl<ECartMapper, ECart> implements IECartService {

    private static final int MAX_QUANTITY = 999;
    private static final long SYNC_DELAY_SECONDS = 5;

    private final ProductClient productClient;
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper rabbitMqHelper;

    // ======================== read ========================

    @Override
    public R<List<CartVO>> getCartList() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        List<ECart> cartItems = lambdaQuery()
                .eq(ECart::getUserId, userId)
                .orderByDesc(ECart::getCreateTime)
                .list();
        if (CollUtils.isEmpty(cartItems)) return R.ok(Collections.emptyList());

        // Redis 数量覆盖：有则用 Redis，无则用 DB 并回写（缓存预热）
        // qty<=0 是 remove 标记的软删除，过滤展示但保留标记（等 MQ 落库后由 syncToDb 清理）
        Map<Object, Object> redisQtys = redisTemplate.opsForHash().entries(CART_QTY_PREFIX + userId);
        Map<String, String> warmUp = new HashMap<>();
        for (ECart item : cartItems) {
            String key = item.getId().toString();
            Object qtyObj = redisQtys.get(key);
            if (qtyObj != null) {
                try { item.setQuantity(Integer.parseInt(qtyObj.toString())); } catch (NumberFormatException ignored) {}
            } else {
                warmUp.put(key, String.valueOf(item.getQuantity()));
            }
            if (item.getQuantity() > CART_ACTIVE_THRESHOLD) {
                redisTemplate.opsForHash().put(CART_OWNER_PREFIX + userId, key, "1");
            }
        }
        if (!warmUp.isEmpty()) {
            redisTemplate.opsForHash().putAll(CART_QTY_PREFIX + userId, warmUp);
        }
        redisTemplate.expire(CART_OWNER_PREFIX + userId, Duration.ofDays(7));
        // 过滤掉已标记删除的条目（qty<=0），但不删 Redis key，避免 warm-up 回写
        cartItems = cartItems.stream()
                .filter(item -> item.getQuantity() > CART_ACTIVE_THRESHOLD)
                .collect(Collectors.toList());

        Set<Long> productIds = cartItems.stream().map(ECart::getProductId).collect(Collectors.toSet());
        Map<Long, ProductVO> productMap = getProductMap(productIds);

        List<CartVO> voList = cartItems.stream()
                .map(cart -> buildCartVO(cart, productMap.get(cart.getProductId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return R.ok(voList);
    }

    // ======================== add ========================

    @Override
    @Transactional
    public R<Void> add(CartAddDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");
        if (dto.getQuantity() == null || dto.getQuantity() < 1) throw new BadRequestException("数量必须大于0");
        if (dto.getQuantity() > MAX_QUANTITY) throw new BadRequestException("单次添加数量不能超过" + MAX_QUANTITY);

        ProductVO product = fetchProduct(dto.getProductId());
        if (product == null) throw new BadRequestException("商品不存在或已下架");

        Long skuId = dto.getSkuId() != null ? dto.getSkuId() : 0L;
        SkuVO sku = findSku(product, skuId);
        if (sku == null && hasRealSkus(product)) throw new BadRequestException("请选择商品规格");

        ECart existing = lambdaQuery()
                .eq(ECart::getUserId, userId)
                .eq(ECart::getProductId, dto.getProductId())
                .eq(ECart::getSkuId, skuId)
                .one();

        if (existing != null) {
            int current = getQuantityFromRedis(userId, existing.getId(), existing.getQuantity());
            int total = current + dto.getQuantity();
            if (total > MAX_QUANTITY)
                throw new BadRequestException("该商品购物车数量已达上限" + MAX_QUANTITY);
            redisTemplate.opsForHash().put(CART_OWNER_PREFIX + userId, existing.getId().toString(), "1");
            redisTemplate.expire(CART_OWNER_PREFIX + userId, Duration.ofDays(7));
            redisTemplate.opsForHash().put(CART_QTY_PREFIX + userId, existing.getId().toString(), String.valueOf(total));
            scheduleSync(userId, existing.getId());
            return R.ok();
        }

        ECart cart = BeanUtils.copyBean(dto, ECart.class);
        cart.setUserId(userId);
        cart.setSkuName(sku != null ? resolveSkuLabel(sku) : null);
        save(cart);
        redisTemplate.opsForHash().put(CART_OWNER_PREFIX + userId, cart.getId().toString(), "1");
        redisTemplate.expire(CART_OWNER_PREFIX + userId, Duration.ofDays(7));
        redisTemplate.opsForHash().put(CART_QTY_PREFIX + userId, cart.getId().toString(), String.valueOf(dto.getQuantity()));
        return R.ok();
    }

    // ======================== update ========================

    @Override
    public R<Void> update(Long cartItemId, Integer quantity) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");
        if (quantity == null || quantity < 1) throw new BadRequestException("数量必须大于0");
        if (quantity > MAX_QUANTITY) throw new BadRequestException("数量不能超过" + MAX_QUANTITY);

        if (!owns(userId, cartItemId)) throw new BadRequestException("购物车记录不存在");

        int current = getQuantity(userId, cartItemId);
        if (quantity.equals(current)) return R.ok();

        redisTemplate.opsForHash().put(CART_QTY_PREFIX + userId, cartItemId.toString(), String.valueOf(quantity));
        scheduleSync(userId, cartItemId);
        return R.ok();
    }

    // ======================== remove ========================

    @Override
    public R<Void> remove(Long cartItemId) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");
        if (!owns(userId, cartItemId)) throw new BadRequestException("购物车记录不存在");

        redisTemplate.opsForHash().put(CART_QTY_PREFIX + userId, cartItemId.toString(), CART_DELETE_MARKER);
        redisTemplate.opsForHash().delete(CART_OWNER_PREFIX + userId, cartItemId.toString());
        scheduleSync(userId, cartItemId);
        return R.ok();
    }

    // ======================== clear ========================

    @Override
    @Transactional
    public R<Void> clear() {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        lambdaUpdate().eq(ECart::getUserId, userId).remove();
        redisTemplate.delete(List.of(CART_QTY_PREFIX + userId, CART_OWNER_PREFIX + userId));
        return R.ok();
    }

    // ======================== package helpers ========================

    private int getQuantity(Long userId, Long cartItemId) {
        Object val = redisTemplate.opsForHash().get(CART_QTY_PREFIX + userId, cartItemId.toString());
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        ECart cart = getById(cartItemId);
        return cart != null ? cart.getQuantity() : 0;
    }

    private int getQuantityFromRedis(Long userId, Long cartItemId, int fallback) {
        Object val = redisTemplate.opsForHash().get(CART_QTY_PREFIX + userId, cartItemId.toString());
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private boolean owns(Long userId, Long cartItemId) {
        Boolean exists = redisTemplate.opsForHash().hasKey(CART_OWNER_PREFIX + userId, cartItemId.toString());
        if (exists != null && exists) return true;
        // Redis miss：回退 DB 并回写（兼容旧数据）
        boolean match = lambdaQuery()
                .eq(ECart::getId, cartItemId)
                .eq(ECart::getUserId, userId)
                .count() > 0;
        if (match) {
            redisTemplate.opsForHash().put(CART_OWNER_PREFIX + userId, cartItemId.toString(), "1");
            redisTemplate.expire(CART_OWNER_PREFIX + userId, Duration.ofDays(7));
        }
        return match;
    }

    /**
     * 发送延迟 MQ 消息：先递增版本号，再发送。消费端比对版本号决定是否落库。
     */
    private void scheduleSync(Long userId, Long cartItemId) {
        String verKey = CART_SYNC_VER_PREFIX + userId + ":" + cartItemId;
        long version = redisTemplate.opsForValue().increment(verKey);
        CartSyncMessage msg = new CartSyncMessage(userId, cartItemId, version);
        rabbitMqHelper.sendAsync(DELAY_EXCHANGE, CART_SYNC_KEY, msg, Duration.ofSeconds(SYNC_DELAY_SECONDS).toMillis());
    }

    public void syncToDb(Long userId, Long cartItemId) {
        try {
            Object val = redisTemplate.opsForHash().get(CART_QTY_PREFIX + userId, cartItemId.toString());
            if (val == null) return;

            int qty;
            try {
                qty = Integer.parseInt(val.toString());
            } catch (NumberFormatException e) {
                log.error("购物车 Redis 数量脏数据，已清理: userId={}, cartItemId={}, val={}", userId, cartItemId, val);
                redisTemplate.opsForHash().delete(CART_QTY_PREFIX + userId, cartItemId.toString());
                return;
            }

            if (qty <= CART_ACTIVE_THRESHOLD) {
                lambdaUpdate().eq(ECart::getId, cartItemId).eq(ECart::getUserId, userId).remove();
                redisTemplate.opsForHash().delete(CART_QTY_PREFIX + userId, cartItemId.toString());
                redisTemplate.opsForHash().delete(CART_OWNER_PREFIX + userId, cartItemId.toString());
            } else {
                lambdaUpdate()
                        .eq(ECart::getId, cartItemId)
                        .eq(ECart::getUserId, userId)
                        .set(ECart::getQuantity, qty)
                        .update();
            }
        } catch (Exception e) {
            log.error("购物车异步落库失败: userId={}, cartItemId={}", userId, cartItemId, e);
        }
    }

    // ======================== product helpers (unchanged) ========================

    private Map<Long, ProductVO> getProductMap(Set<Long> productIds) {
        try {
            R<List<ProductVO>> response = productClient.getDetailsByIds(productIds);
            if (response == null || !response.success() || CollUtils.isEmpty(response.getData())) {
                log.warn("Feign 批量查商品返回空: ids={}", productIds);
                return Collections.emptyMap();
            }
            return response.getData().stream().collect(Collectors.toMap(ProductVO::getId, p -> p));
        } catch (Exception e) {
            log.error("Feign 批量查商品异常: ids={}", productIds, e);
            return Collections.emptyMap();
        }
    }

    private ProductVO fetchProduct(Long productId) {
        try {
            R<ProductVO> response = productClient.getDetail(productId);
            if (response == null || !response.success() || response.getData() == null) return null;
            return response.getData();
        } catch (Exception e) {
            log.error("Feign 查商品详情异常: productId={}", productId, e);
            return null;
        }
    }

    private boolean hasRealSkus(ProductVO product) {
        if (CollUtils.isEmpty(product.getSkus())) return false;
        return product.getSkus().stream().anyMatch(s -> s.getId() != null && s.getId() != 0);
    }

    private SkuVO findSku(ProductVO product, Long skuId) {
        if (CollUtils.isEmpty(product.getSkus())) return null;
        if (skuId == null || skuId == 0) {
            if (product.getSkus().size() == 1 && product.getSkus().get(0).getId() == 0) return product.getSkus().get(0);
            return null;
        }
        return product.getSkus().stream().filter(s -> skuId.equals(s.getId())).findFirst().orElse(null);
    }

    private String resolveSkuLabel(SkuVO sku) {
        if (CollUtils.isEmpty(sku.getSpecs())) return null;
        return sku.getSpecs().stream().map(SkuVO.SpecVO::getValue).collect(Collectors.joining(" "));
    }

    private CartVO buildCartVO(ECart cart, ProductVO product) {
        if (product == null) {
            CartVO fallback = BeanUtils.copyBean(cart, CartVO.class);
            fallback.setCartItemId(cart.getId());
            return fallback;
        }
        CartVO vo = BeanUtils.copyBean(cart, CartVO.class);
        vo.setCartItemId(cart.getId());
        vo.setProductName(product.getName());
        vo.setProductImage(product.getImage());
        vo.setShopId(product.getShopId());
        vo.setShopName(product.getShopName());

        SkuVO matchedSku = findSku(product, cart.getSkuId());
        if (matchedSku != null) {
            vo.setPrice(matchedSku.getPrice());
            if (cart.getSkuName() == null) vo.setSkuName(resolveSkuLabel(matchedSku));
        }
        return vo;
    }
}
