package com.ecommerce.order.constants;

public interface RedisConstants {
    /** 购物车数量 Hash，key 模式: cart:qty:{userId} */
    String CART_QTY_PREFIX = "cart:qty:";
    /** 购物车归属 Hash，key 模式: cart:owner:{userId}，hashKey: cartItemId */
    String CART_OWNER_PREFIX = "cart:owner:";
    /** 购物车 MQ 落库版本号，key 模式: cart:sync:ver:{userId}:{cartItemId} */
    String CART_SYNC_VER_PREFIX = "cart:sync:ver:";
    /** 删除标记值：写入 Redis 占位，防止 warm-up 回写，等 MQ 落库后由 syncToDb 清理 */
    String CART_DELETE_MARKER = "0";
    /** 购物车有效数量下限，低于此值视为已删除 */
    int CART_ACTIVE_THRESHOLD = 0;

    /** 优惠券元数据 Hash（stock + status），key 模式: coupon:meta:{couponId} */
    String COUPON_META_PREFIX = "coupon:meta:";
    /** 用户已领取某券数量，key 模式: coupon:user:claimed:{couponId}:{userId} */
    String COUPON_USER_CLAIMED_PREFIX = "coupon:user:claimed:";
    /** 用户券包缓存，key 模式: user:coupons:{userId} */
    String USER_COUPONS_PREFIX = "user:coupons:";
    /** 用户券包缓存过期时间（分钟） */
    long USER_COUPONS_TTL_MINUTES = 10;

    /** SKU 库存，key 模式: product:sku:stock:{skuId} */
    String SKU_STOCK_PREFIX = "product:sku:stock:";
    /** MQ 消息去重，key 模式: mq:dedup:{messageId}，TTL 24h */
    String MQ_DEDUP_PREFIX = "mq:dedup:";
}
