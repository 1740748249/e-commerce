package com.ecommerce.common.constants;

public interface MqConstants {
    interface Exchange {
        /* 订单相关的交换机 */
        String ORDER_EXCHANGE = "order.topic";

        /* 商品相关的交换机 */
        String PRODUCT_EXCHANGE = "product.topic";

        /* 用户相关的交换机 */
        String USER_EXCHANGE = "user.topic";

        /* 社交相关的交换机 */
        String SOCIAL_EXCHANGE = "social.topic";

        /* 异常信息的交换机 */
        String ERROR_EXCHANGE = "error.topic";

        /* 支付有关的交换机 */
        String PAY_EXCHANGE = "pay.topic";

        /* 延迟任务交换机 */
        String DELAY_EXCHANGE = "delay.topic";

        /* 优惠券相关的交换机 */
        String COUPON_EXCHANGE = "coupon.topic";
    }

    interface Queue {
        String ERROR_QUEUE_TEMPLATE = "error.{}.queue";
    }

    interface Key {
        /* 订单相关的 RoutingKey */
        String ORDER_CREATE_KEY = "order.create";
        String ORDER_PAY_KEY = "order.pay";
        String ORDER_CANCEL_KEY = "order.cancel";
        String ORDER_REFUND_KEY = "order.refund";

        /* 商品相关的 RoutingKey */
        String PRODUCT_NEW_KEY = "product.new";
        String PRODUCT_UPDATE_KEY = "product.update";
        String PRODUCT_DELETE_KEY = "product.delete";
        String PRODUCT_STOCK_CHANGE_KEY = "product.stock.change";

        /* 用户相关的 RoutingKey */
        String USER_REGISTER_KEY = "user.register";

        /* 社交相关的 RoutingKey */
        String LIKED_TIMES_KEY_TEMPLATE = "{}.times.changed";
        String PRODUCT_LIKED_TIMES_KEY = "PRODUCT.times.changed";

        /* 异常 RoutingKey 的前缀 */
        String ERROR_KEY_PREFIX = "error.";
        String DEFAULT_ERROR_KEY = "error.#";

        /* 支付有关的 key */
        String PAY_SUCCESS = "pay.success";
        String REFUND_CHANGE = "refund.status.change";

        String ORDER_DELAY_KEY = "delay.order.query";

        /* 购物车延迟落库 */
        String CART_SYNC_KEY = "cart.sync";

        /* 优惠券异步落库 */
        String COUPON_CLAIM_KEY = "coupon.claim";

        /* 库存同步落库 */
        String STOCK_SYNC_KEY = "stock.sync";

        /* 库存恢复（订单取消等） */
        String STOCK_RESTORE_KEY = "stock.restore";

        /* 秒杀异步创建订单 */
        String ORDER_FLASH_CREATE_KEY = "order.flash.create";

        /* 订单通知（order-service → notification-service） */
        String ORDER_NOTIFY_KEY = "order.notify";

        /**
         * 订单取消通知（order-service → product-service）。
         * <p>
         * 生产者：{@code EOrderServiceImpl.cancel()}，检测到 flashSaleOrderId != null 时发出。
         * 消费者：{@code OrderCancelledListener.onOrderCancelled()}，
         * 查 EFlashSaleOrder → 乐观锁取消 → Lua 回补 Redis 秒杀库存 → 发 restore MQ 落 DB。
         */
        String ORDER_CANCELLED_NOTIFY_KEY = "order.cancelled.notify";

        /**
         * 秒杀库存同步落库（扣减）。
         * <p>
         * 生产者：{@code EFlashSaleServiceImpl.order()}，在 Redis 扣减且主 MQ 发送成功后发出。
         * 消费者：{@code FlashStockSyncListener.onSync()}，DB 执行 {@code stock = stock - quantity}
         * 并带 {@code WHERE stock >= quantity} 防超卖。
         */
        String FLASH_STOCK_SYNC_KEY = "flash.stock.sync";

        /**
         * 秒杀库存回补落库（取消 / 超时 / DLQ）。
         * <p>
         * 生产者：{@code OrderCancelledListener}、{@code FlashSaleOrderDlqListener}、
         * {@code FlashSaleTimeoutJob}，均在 Lua 补偿成功后发出。
         * 消费者：{@code FlashStockSyncListener.onRestore()}，DB 执行 {@code stock = stock + quantity}。
         */
        String FLASH_STOCK_RESTORE_KEY = "flash.stock.restore";

        // 秒杀退款通知（order-service → product-service），回补 Redis FLASH_STOCK 并落 DB
        String FLASH_SALE_REFUND_KEY = "flash.sale.refund";

        /**
         * 普通订单创建失败，Redis 回滚失败后的 DLQ 兜底通知。
         * <p>
         * 生产者：{@code EOrderServiceImpl.create()} catch 块中 Redis 回滚异常时发出。
         * 消费者：{@code OrderCreateFailedDlqListener}，去重后 pipeline INCR 回补库存。
         */
        String ORDER_CREATE_FAILED_KEY = "order.create.failed";

        /**
         * 秒杀订单支付成功通知（order-service → product-service）。
         * <p>
         * 生产者：{@code EOrderServiceImpl.payCallback()}，订单有 flashSaleOrderId 时发出。
         * 消费者：{@code FlashOrderPaidListener.onFlashOrderPaid()}，
         * 将 EFlashSaleOrder.status 更新为 PAID，防止 FlashSaleTimeoutJob 误回补库存。
         */
        String FLASH_ORDER_PAID_KEY = "flash.order.paid";
    }
}
