package com.ecommerce.notification.constants;

public class RedisConstants {
    private RedisConstants() {}

    /** 店铺未读通知数，key: shop:{shopId}:unread，value: int */
    public static final String SHOP_UNREAD_KEY = "shop:%d:unread";

    /** 通知幂等去重，key: notify:dedup:{orderNo}:{type}，用于 SET NX */
    public static final String NOTIFY_DEDUP_KEY = "notify:dedup:%d:%d";

    /** 管理员通知去重，key: notify:admin:dedup:{shopId}:{type}:{titleHash}，用于 SET NX，TTL 2小时 */
    public static final String NOTIFY_ADMIN_DEDUP_KEY = "notify:admin:dedup:%d:%d:%s";

    /** 未读计数缓存时间：10 分钟（过期后下次查询从 DB 重建） */
    public static final long UNREAD_TTL_SECONDS = 600;

    /** 去重 key 过期时间：24 小时（远超 MQ 重投窗口，避免 Redis 内存泄漏） */
    public static final long DEDUP_TTL_SECONDS = 86400;

    /** 管理员通知去重 TTL：2 小时 */
    public static final long ADMIN_DEDUP_TTL_SECONDS = 7200;

    /** 广播去重，key: notify:broadcast:dedup:{titleHash}，TTL 1小时 */
    public static final String NOTIFY_BROADCAST_DEDUP_KEY = "notify:broadcast:dedup:%s";
    public static final long BROADCAST_DEDUP_TTL_SECONDS = 3600;

}
