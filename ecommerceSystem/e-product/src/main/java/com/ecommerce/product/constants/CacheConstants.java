package com.ecommerce.product.constants;

import java.time.Duration;

public final class CacheConstants {
    private CacheConstants() {}

    /** 缓存穿透空值 TTL */
    public static final Duration CACHE_THROUGH_TTL = Duration.ofMinutes(5);

    /** 全部分类缓存 Hash，field=categoryId，value=EFlashSession JSON */
    public static final String CATEGORY_ALL_KEY = "product:category:all";
    public static final Duration CATEGORY_TTL = Duration.ofHours(12);

    /** 全部店铺缓存 Hash，field=shopId，value=EProduct JSON */
    public static final String SHOP_ALL_KEY = "product:shop:all";
    public static final Duration SHOP_TTL = Duration.ofMinutes(30);

    /** 商品排行榜 ZSet，member=productId，score=销量 */
    public static final String RANKING_KEY_PREFIX = "product:ranking";
    public static final Duration RANKING_TTL = Duration.ofMinutes(5);

    /** 用户-店铺映射缓存 */
    public static final String USER_SHOP_RELATED_KEY = "user:shop:mapping";

    /** 店铺商品列表 String，JSON 数组，key=shop:products:list:{shopId} */
    public static final String shop_products_list_key_prefix = "shop:products:list:";
    public static final Duration shop_products_list_ttl = Duration.ofMinutes(20);

    /** 全部秒杀场次缓存 Hash，field=sessionId，value=EFlashSession JSON */
    public static final String SESSION_ALL_KEY = "product:session:all";
    public static final Duration SESSION_TTL = Duration.ofMinutes(30);

    // ==================== 库存 ====================

    /** SKU 库存 String，key=product:sku:stock:{skuId} */
    public static final String SKU_STOCK_PREFIX = "product:sku:stock:";

    // ==================== MQ ====================

    /** MQ 幂等标记前缀，key=mq:dedup:{messageId}，TTL 24h */
    public static final String MQ_DEDUP_PREFIX = "mq:dedup:";

    // ==================== 秒杀 ====================

    /** 秒杀库存 String，key=flash:stock:{flashSaleId}，TTL=场次结束后 1h */
    public static final String FLASH_STOCK_PREFIX = "flash:stock:";
    /** 用户已购计数 Hash，key=flash:user:{flashSaleId}，field=userId，TTL=场次结束后 1h */
    public static final String FLASH_USER_PREFIX = "flash:user:";
    /** 秒杀最低价 SKU ID String，key=flash:sku:{flashSaleId}，TTL=场次结束后 1h */
    public static final String FLASH_SKU_PREFIX = "flash:sku:";
    /** 秒杀活动缓存 String，key=flash:sale:{flashSaleId}，value=EFlashSale JSON */
    public static final String FLASH_SALE_PREFIX = "flash:sale:";
    /** 秒杀订单缓存 String，key=flash:order:{flashSaleId}:{userId}，value=EFlashSaleOrder JSON */
    public static final String FLASH_ORDER_PREFIX = "flash:order:";
    /** 预热完成标记 String，key=flash:warmed:{flashSaleId}，TTL=场次持续时间 */
    public static final String FLASH_WARMED_PREFIX = "flash:warmed:";
}
