package com.ecommerce.notification.constants;

public class RedisConstants {
    private RedisConstants() {}

    /** 店铺未读通知数，key: shop:{shopId}:unread，value: int */
    public static final String SHOP_UNREAD_KEY = "shop:%d:unread";

    /** 通知幂等去重，key: notify:dedup:{orderNo}:{type}，用于 SET NX */
    public static final String NOTIFY_DEDUP_KEY = "notify:dedup:%d:%d";

    /** 未读计数缓存时间：10 分钟（过期后下次查询从 DB 重建） */
    public static final long UNREAD_TTL_SECONDS = 600;

    /** 去重 key 过期时间：24 小时（远超 MQ 重投窗口，避免 Redis 内存泄漏） */
    public static final long DEDUP_TTL_SECONDS = 86400;
}
