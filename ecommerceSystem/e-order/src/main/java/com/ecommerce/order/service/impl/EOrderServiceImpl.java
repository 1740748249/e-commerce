package com.ecommerce.order.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.client.UserClient;
import com.ecommerce.api.dto.AddressDTO;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.api.dto.ProductVO;
import com.ecommerce.api.dto.SkuVO;
import com.ecommerce.api.dto.StockSyncBatchMessage;
import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.api.message.FlashOrderPaidMessage;
import com.ecommerce.api.message.OrderCancelledMessage;
import com.ecommerce.api.message.OrderCreateFailedMessage;
import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.common.autoconfigure.mq.RabbitMqHelper;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.order.domain.dto.*;
import com.ecommerce.order.domain.po.ECoupon;
import com.ecommerce.order.domain.po.EOrder;
import com.ecommerce.order.domain.po.EOrderItem;
import com.ecommerce.order.domain.po.EUserCoupon;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.enums.UserCouponStatus;
import com.ecommerce.order.mapper.EOrderMapper;
import com.ecommerce.order.query.OrderPageQuery;
import com.ecommerce.order.domain.vo.*;
import com.ecommerce.order.service.IECouponService;
import com.ecommerce.order.service.IEOrderItemService;
import com.ecommerce.order.service.IEOrderService;
import com.ecommerce.order.service.IEUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.common.constants.MqConstants.Exchange.*;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_ORDER_PAID_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_SALE_REFUND_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_CANCELLED_NOTIFY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_CREATE_FAILED_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_DELAY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_NOTIFY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_PAY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_RESTORE_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_SYNC_KEY;
import static com.ecommerce.order.constants.RedisConstants.SKU_STOCK_PREFIX;

@Service
@RequiredArgsConstructor
@Slf4j
public class EOrderServiceImpl extends ServiceImpl<EOrderMapper, EOrder> implements IEOrderService {

    private final IEOrderItemService orderItemService;
    private final IEUserCouponService userCouponService;
    private final IECouponService couponService;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> deductSkuStockBatchScript;
    private final RabbitMqHelper rabbitMqHelper;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public R<Long> create(CreateOrderDTO dto) {
        // 0. 获取当前登录用户
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        // 1. 校验收货地址 —— 通过 Feign 远程调用 user 服务查询地址是否存在
        R<AddressDTO> addrR = userClient.getAddressById(dto.getAddressId());
        if (!addrR.success() || addrR.getData() == null) {
            throw new BadRequestException("收货地址不存在");
        }
        AddressDTO addr = addrR.getData();

        // 2. 批量查询商品信息 —— 一次性查所有商品，避免逐个 Feign 调用的 N+1 问题
        Set<Long> productIds = dto.getItems().stream()
                .map(OrderItemDTO::getProductId).collect(Collectors.toSet());
        R<List<ProductVO>> prodR = productClient.getDetailsByIds(productIds);
        if (!prodR.success() || CollUtils.isEmpty(prodR.getData())) {
            throw new BadRequestException("商品信息查询失败");
        }
        // 转为 Map<productId, ProductVO> 方便后续 O(1) 查找
        Map<Long, ProductVO> prodMap = prodR.getData().stream()
                .collect(Collectors.toMap(ProductVO::getId, p -> p));

        // 3. 遍历购物车条目：校验商品/SKU、构建订单快照、按 SKU 合并扣库存参数、按店铺分组
        // shopItemsMap: shopId → 该店铺的订单明细列表（不同店铺生成不同订单）
        Map<Long, List<EOrderItem>> shopItemsMap = new LinkedHashMap<>();
        // shopTotalMap: shopId → 该店铺的商品总金额（用于优惠券门槛校验）
        Map<Long, Integer> shopTotalMap = new LinkedHashMap<>();
        // skuQtyMap: skuId → 累计购买数量（同一 SKU 多次添加时合并，减少 Lua 扣减次数）
        Map<Long, Integer> skuQtyMap = new LinkedHashMap<>();
        // skuStockMap: skuId → 原始库存（仅首次放入，用于 Lua 判断库存是否充足）
        Map<Long, Integer> skuStockMap = new HashMap<>();
        // skuNameMap: skuId → 商品名（仅首次放入，用于库存不足时提示具体商品）
        Map<Long, String> skuNameMap = new HashMap<>();

        for (OrderItemDTO item : dto.getItems()) {
            // 3.1 校验购买数量（DTO 只有 @NotNull，没有 @Min，在此兜底校验）
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BadRequestException("商品数量必须大于0");
            }
            // 3.2 校验商品是否存在且已上架（status=1 表示上架）
            ProductVO product = prodMap.get(item.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BadRequestException("商品「" + (product != null ? product.getName() : item.getProductId()) + "」已下架");
            }

            // 3.2 校验 SKU 是否存在且未停用
            SkuVO sku = product.getSkus().stream()
                    .filter(s -> s.getId().equals(item.getSkuId())
                            && (s.getStatus() == null || s.getStatus() == 1))
                    .findFirst().orElse(null);
            if (sku == null) {
                throw new BadRequestException("商品「" + product.getName() + "」的 SKU 不存在或已停用");
            }

            // 3.3 按 SKU 维度合并购买数量（同一 SKU 可能出现在多个 OrderItemDTO 中）
            skuQtyMap.merge(sku.getId(), item.getQuantity(), Integer::sum);
            // 记录该 SKU 的原始库存，用于 Lua 脚本做库存充足性校验
            skuStockMap.putIfAbsent(sku.getId(), sku.getStock());
            // 记录 SKU 对应的商品名，用于库存不足时给出可读的错误提示
            skuNameMap.putIfAbsent(sku.getId(), product.getName());

            // 3.4 构建订单明细快照 —— 下单时固化商品信息，后续商品变更不影响历史订单
            EOrderItem orderItem = new EOrderItem();
            BeanUtils.copyProperties(item, orderItem);
            orderItem.setProductName(product.getName());
            // SKU 规格名：将 Map<规格名, 规格值> 拼成 "颜色:红; 尺寸:L" 的字符串存入快照
            orderItem.setSkuName(sku.getSpecs() != null ? sku.getSpecs().stream()
                    .map(s -> s.getName() + ":" + s.getValue())
                    .collect(Collectors.joining("; ")) : "");
            // 优先用 SKU 级别的图片，没有则回退到商品主图
            orderItem.setProductImage(sku.getImage() != null ? sku.getImage() : product.getImage());
            orderItem.setPrice(sku.getPrice());       // 快照下单时的 SKU 价格
            orderItem.setShopId(product.getShopId()); // 记录店铺 ID，后续按店铺拆分订单

            // 3.5 按店铺分组：同一店铺的商品归入同一笔订单
            shopItemsMap.computeIfAbsent(product.getShopId(), k -> new ArrayList<>()).add(orderItem);
            // 累加该店铺的商品金额
            shopTotalMap.merge(product.getShopId(), sku.getPrice() * item.getQuantity(), Integer::sum);
        }

        // 3.6 组装 Lua 脚本参数 —— KEYS: 每个 SKU 的 Redis key; ARGV: 交替存放扣减量和原始库存
        List<String> stockKeys = new ArrayList<>();
        List<String> stockArgs = new ArrayList<>();
        // skuIdOrder 保持与 Lua KEYS 数组相同的顺序，用于负数返回值反查是哪个 SKU 库存不足
        List<Long> skuIdOrder = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : skuQtyMap.entrySet()) {
            stockKeys.add(SKU_STOCK_PREFIX + entry.getKey());
            stockArgs.add(String.valueOf(entry.getValue()));
            stockArgs.add(String.valueOf(skuStockMap.get(entry.getKey())));
            skuIdOrder.add(entry.getKey());
        }

        // 3.7 执行 Lua 脚本 —— 一次 Redis 调用批量原子扣库存（all-or-nothing）
        // Lua 返回值：>0 成功（库存充足条数），<0 失败（绝对值表示第几个 SKU 库存不足）
        Long stockResult = redisTemplate.execute(deductSkuStockBatchScript,
                stockKeys, stockArgs.toArray(new String[0]));
        if (stockResult == null) {
            throw new BadRequestException("库存扣减失败，请稍后重试");
        }
        if (stockResult < 0) {
            // Lua 返回负数时，|result| - 1 即为库存不足的 SKU 下标
            int idx = Math.abs(stockResult.intValue()) - 1;
            throw new BadRequestException("商品「" + skuNameMap.get(skuIdOrder.get(idx)) + "」库存不足");
        }

        // 3.8 构建库存同步 MQ 消息体 —— 去重后的 SKU 扣减列表，用于异步同步到 DB
        List<StockSyncBatchMessage.SkuItem> mergedSkuItems = skuQtyMap.entrySet().stream()
                .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 3.9 确定优惠券应作用于哪个店铺 —— 取金额最高的店铺订单享受优惠
        Long maxShopId = shopTotalMap.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Long firstOrderNo = null;   // 记录首笔订单号，用于 buyNow 跳转
        // 4. 以下 DB 操作放在 try 块内：任何异常需回滚 Redis 库存（库存已在 3.7 扣除）
        try {
            boolean couponUsed = false; // 保证一张优惠券只用一次

            // 4.1 按店铺遍历，每个店铺生成一笔独立的订单
            for (Map.Entry<Long, List<EOrderItem>> entry : shopItemsMap.entrySet()) {
                Long shopId = entry.getKey();
                List<EOrderItem> shopItems = entry.getValue();
                int shopTotal = shopTotalMap.get(shopId);

                // 4.2 雪花算法生成全局唯一订单号
                long orderNo = snowflake.nextId();

                // 4.3 优惠券处理 —— 仅对该店铺（最高金额的）使用一次
                int discountAmount = 0;
                if (!couponUsed && dto.getCouponId() != null && shopId.equals(maxShopId)) {
                    // 查询用户持有的优惠券记录
                    EUserCoupon uc = userCouponService.lambdaQuery()
                            .eq(EUserCoupon::getId, dto.getCouponId()).one();
                    if (uc == null || !uc.getUserId().equals(userId)) {
                        throw new BadRequestException("优惠券不存在");
                    }
                    if (uc.getStatus() != UserCouponStatus.UNUSED) {
                        throw new BadRequestException("优惠券已使用或已过期");
                    }
                    if (uc.getExpireAt() != null && uc.getExpireAt().isBefore(LocalDateTime.now())) {
                        throw new BadRequestException("优惠券已过期");
                    }
                    // 查询优惠券模板（满减门槛、减免金额）
                    ECoupon template = couponService.lambdaQuery()
                            .eq(ECoupon::getId, uc.getCouponId()).one();
                    if (template == null) {
                        throw new BadRequestException("优惠券模板不存在");
                    }
                    // 满减门槛校验 —— 使用该店铺的金额，而非全局总金额
                    if (template.getThreshold() != null && template.getThreshold() > 0
                            && shopTotal < template.getThreshold()) {
                        throw new BadRequestException("未达到优惠券使用门槛");
                    }
                    // 乐观锁更新优惠券状态：UNUSED → USED
                    boolean used = userCouponService.lambdaUpdate()
                            .eq(EUserCoupon::getId, uc.getId())
                            .eq(EUserCoupon::getStatus, UserCouponStatus.UNUSED)
                            .set(EUserCoupon::getStatus, UserCouponStatus.USED)
                            .set(EUserCoupon::getUsedAt, LocalDateTime.now())
                            .set(EUserCoupon::getOrderNo, orderNo)
                            .update();
                    if (!used) throw new BadRequestException("优惠券使用失败");
                    discountAmount = template.getReduce() != null ? template.getReduce() : 0;
                    couponUsed = true;
                }

                // 4.4 保存订单主表 —— 复制 DTO 和地址信息，填充快照字段
                EOrder order = new EOrder();
                order.setRemark(dto.getRemark());
                order.setAddressId(dto.getAddressId());
                order.setCouponId(dto.getCouponId());
                order.setReceiverName(addr.getReceiverName());
                order.setReceiverPhone(addr.getReceiverPhone());
                order.setOrderNo(orderNo);
                order.setUserId(userId);
                order.setTotalAmount(shopTotal);
                order.setDiscountAmount(discountAmount);
                order.setStatus(OrderStatus.PENDING_PAYMENT); // 初始状态：待支付
                // 拼接完整收货地址字符串
                order.setReceiverAddr(addr.getProvince() + addr.getCity() + addr.getDistrict() + addr.getDetail());
                order.setShopId(shopId);
                // 非优惠券订单不应记录 couponId（同一请求可能包含多店铺商品，优惠券只用于一笔订单）
                if (discountAmount == 0) {
                    order.setCouponId(null);
                }
                save(order); // MyBatis-Plus 插入，执行后 order.getId() 被自动回填
                if (firstOrderNo == null) firstOrderNo = orderNo;

                // 4.5 保存订单明细 —— 将主表 ID 回填到每条明细，然后批量插入
                for (EOrderItem item : shopItems) {
                    item.setOrderId(order.getId());
                }
                orderItemService.saveBatch(shopItems);

                // 4.6 发送订单超时延迟消息 —— 30 分钟后未支付则自动取消并归还库存
                // 注册到事务同步器：只有 DB 事务提交成功后才发送，避免回滚后依然发送消息
                final long finalOrderNo = orderNo;
                // 汇总该订单内各 SKU 的扣减量，用于超时取消时归还正确数量的库存
                final List<StockSyncBatchMessage.SkuItem> shopSkuItems = shopItems.stream()
                        .collect(Collectors.groupingBy(EOrderItem::getSkuId,
                                LinkedHashMap::new, Collectors.summingInt(EOrderItem::getQuantity)))
                        .entrySet().stream()
                        .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    rabbitMqHelper.sendDelayMessage(DELAY_EXCHANGE, ORDER_DELAY_KEY,
                                            OrderTimeoutMessage.builder()
                                                    .orderNo(finalOrderNo)
                                                    .shopId(shopId)
                                                    .totalAmount(order.getTotalAmount())
                                                    .items(shopSkuItems.stream().map(i ->
                                                            new OrderTimeoutMessage.SkuItem(i.getSkuId(), i.getQuantity()))
                                                            .collect(Collectors.toList()))
                                                    .build(),
                                            Duration.ofMinutes(30));
                                } catch (Exception ex) {
                                    log.error("发送订单超时延迟消息失败: orderNo={}", finalOrderNo, ex);
                                }
                            }
                        });

                // 4.7 发送订单通知 MQ —— 通知 notification-service 创建新订单通知
                final int notifyTotal = order.getTotalAmount();
                final String firstProductName = shopItems.get(0).getProductName();
                final int notifyItemCount = shopItems.size();
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                String content = firstProductName
                                        + (notifyItemCount > 1 ? " 等" + notifyItemCount + "件" : "")
                                        + "，金额 ¥" + String.format("%.2f", notifyTotal / 100.0);
                                OrderNotificationMessage msg = OrderNotificationMessage.builder()
                                        .orderNo(finalOrderNo)
                                        .shopId(shopId)
                                        .type(NotificationType.NEW_ORDER)
                                        .title("新订单通知")
                                        .content(content)
                                        .build();
                                rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, msg);
                            }
                        });
            }

            // 4.8 发送全量库存同步 MQ —— 告知商品服务将 Redis 库存同步到 DB
            final String syncMsgId = IdUtil.fastSimpleUUID();
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, STOCK_SYNC_KEY,
                                    new StockSyncBatchMessage(syncMsgId, mergedSkuItems));
                        }
                    });

        } catch (Exception e) {
            // 5. DB 操作失败 → 回滚 Redis 库存（Pipelined 批量 INCRBY 归还）
            log.error("订单创建失败，回滚 Redis 库存: {}", e.getMessage());
            try {
                redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @Override
                    public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                        for (StockSyncBatchMessage.SkuItem item : mergedSkuItems) {
                            ops.opsForValue().increment(SKU_STOCK_PREFIX + item.getSkuId(), item.getQuantity());
                        }
                        return null;
                    }
                });
            } catch (Exception rollbackEx) {
                // Redis 回滚也失败 → 发 DLQ 兜底，让 product-service 重试回补
                log.error("回滚 Redis 库存失败，发送 DLQ 兜底: {}", rollbackEx.getMessage());
                try {
                    rabbitMqHelper.sendAsync(ERROR_EXCHANGE, ORDER_CREATE_FAILED_KEY,
                            OrderCreateFailedMessage.builder()
                                    .messageId(IdUtil.fastSimpleUUID())
                                    .items(mergedSkuItems.stream()
                                            .map(i -> new OrderCreateFailedMessage.SkuItem(
                                                    i.getSkuId(), i.getQuantity()))
                                            .collect(Collectors.toList()))
                                    .createTime(LocalDateTime.now())
                                    .build());
                } catch (Exception mqEx) {
                    log.error("发送 DLQ 消息也失败，库存彻底丢失需人工介入: {}", mqEx.getMessage());
                }
            }
            throw e; // 重新抛出，让 @Transactional 回滚 DB
        }

        return R.ok(firstOrderNo);
    }

    @Override
    @Transactional
    public R<Long> buyNow(BuyNowDTO dto) {
        CreateOrderDTO createDTO = new CreateOrderDTO();
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(dto.getProductId());
        item.setSkuId(dto.getSkuId());
        item.setQuantity(dto.getQuantity());
        createDTO.setItems(List.of(item));
        createDTO.setAddressId(dto.getAddressId());
        createDTO.setCouponId(dto.getCouponId());
        createDTO.setRemark(dto.getRemark());
        return create(createDTO);
    }

    @Override
    public R<OrderPreviewVO> preview(OrderPreviewDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        // 批量查商品 + 按 SKU 匹配价格
        Set<Long> productIds = dto.getItems().stream()
                .map(OrderItemDTO::getProductId).collect(Collectors.toSet());
        R<List<ProductVO>> prodR = productClient.getDetailsByIds(productIds);
        if (!prodR.success() || CollUtils.isEmpty(prodR.getData())) {
            throw new BadRequestException("商品信息查询失败");
        }
        Map<Long, ProductVO> prodMap = prodR.getData().stream()
                .collect(Collectors.toMap(ProductVO::getId, p -> p));

        List<PreviewItemVO> previewItems = new ArrayList<>();
        int totalAmount = 0;
        for (OrderItemDTO item : dto.getItems()) {
            ProductVO product = prodMap.get(item.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BadRequestException("商品「" + (product != null ? product.getName() : item.getProductId()) + "」已下架");
            }
            SkuVO sku = product.getSkus().stream()
                    .filter(s -> s.getId().equals(item.getSkuId())
                            && (s.getStatus() == null || s.getStatus() == 1))
                    .findFirst().orElse(null);
            if (sku == null) {
                throw new BadRequestException("商品「" + product.getName() + "」的 SKU 不存在或已停用");
            }
            int subtotal = sku.getPrice() * item.getQuantity();
            totalAmount += subtotal;

            PreviewItemVO piv = new PreviewItemVO();
            piv.setProductId(product.getId());
            piv.setProductName(product.getName());
            piv.setSkuId(sku.getId());
            piv.setPrice(sku.getPrice());
            piv.setQuantity(item.getQuantity());
            piv.setSubtotal(subtotal);
            // skuName: 有规格则拼规格值，无规格则为 null
            if (sku.getSpecs() != null && !sku.getSpecs().isEmpty()) {
                piv.setSkuName(sku.getSpecs().stream()
                        .map(s -> s.getName() + ":" + s.getValue())
                        .collect(Collectors.joining("; ")));
            }
            previewItems.add(piv);
        }

        // 校验优惠券
        int discountAmount = 0;
        if (dto.getCouponId() != null) {
            EUserCoupon uc = userCouponService.lambdaQuery().eq(EUserCoupon::getId, dto.getCouponId()).one();
            if (uc == null || !uc.getUserId().equals(userId)) {
                throw new BadRequestException("优惠券不存在");
            }
            if (uc.getStatus() != UserCouponStatus.UNUSED) {
                throw new BadRequestException("优惠券已使用或已过期");
            }
            if (uc.getExpireAt() != null && uc.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("优惠券已过期");
            }
            ECoupon template = couponService.lambdaQuery().eq(ECoupon::getId, uc.getCouponId()).one();
            if (template == null) {
                throw new BadRequestException("优惠券模板不存在");
            }
            if (template.getThreshold() != null && template.getThreshold() > 0
                    && totalAmount < template.getThreshold()) {
                throw new BadRequestException("未达到优惠券使用门槛");
            }
            discountAmount = template.getReduce() != null ? template.getReduce() : 0;
        }

        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(previewItems);
        vo.setTotalAmount(totalAmount);
        vo.setDiscountAmount(discountAmount);
        vo.setPayAmount(totalAmount - discountAmount);
        return R.ok(vo);
    }

    @Override
    public R<PageDTO<OrderVO>> myOrders(OrderPageQuery query) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        // 分页查订单主表
        Page<EOrder> page = lambdaQuery()
                .eq(EOrder::getUserId, userId)
                .eq(query.getStatus() != null, EOrder::getStatus, query.getStatus())
                .page(query.toMpPageDefaultSortByCreateTimeDesc());

        List<EOrder> orders = page.getRecords();
        if (CollUtils.isEmpty(orders)) {
            return R.ok(PageDTO.of(page, CollUtils.emptyList()));
        }

        // 批量查所有订单的商品明细
        List<Long> orderIds = orders.stream().map(EOrder::getId).collect(Collectors.toList());
        List<EOrderItem> allItems = orderItemService.lambdaQuery().in(EOrderItem::getOrderId, orderIds).list();
        Map<Long, List<EOrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(EOrderItem::getOrderId));

        List<OrderVO> records = orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setPayAmount(order.getTotalAmount() - order.getDiscountAmount());
            vo.setStatusText(order.getStatus() != null ? order.getStatus().getDesc() : null);
            vo.setReceiverPhone(maskPhone(order.getReceiverPhone()));
            vo.setReceiverAddr(maskAddr(order.getReceiverAddr()));

            List<EOrderItem> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(PageDTO.of(page, records));
    }

    @Override
    public R<OrderDetailVO> detail(Long orderNo) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");

        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BadRequestException("订单不存在");
        }

        List<EOrderItem> items = orderItemService.lambdaQuery().eq(EOrderItem::getOrderId, order.getId()).list();

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setPayAmount(order.getTotalAmount() - order.getDiscountAmount());
        vo.setStatusText(order.getStatus() != null ? order.getStatus().getDesc() : null);
        // 详情不脱敏，保留全量信息
        vo.setReceiverFullName(order.getReceiverName());
        vo.setReceiverFullPhone(order.getReceiverPhone());
        vo.setReceiverFullAddr(order.getReceiverAddr());
        vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
        return R.ok(vo);
    }

    @Override
    public R<PageDTO<OrderVO>> shopOrders(OrderPageQuery query) {
        Long shopId = UserContext.getShopId();
        if (shopId == null) throw new BizIllegalException("非商家账号");

        Page<EOrder> page = lambdaQuery()
                .eq(EOrder::getShopId, shopId)
                .eq(query.getStatus() != null, EOrder::getStatus, query.getStatus())
                .orderByDesc(EOrder::getCreateTime)
                .page(new Page<>(query.getPage(), query.getSize()));

        List<EOrder> orders = page.getRecords();
        if (CollUtils.isEmpty(orders)) {
            return R.ok(PageDTO.of(page, Collections.emptyList()));
        }

        List<Long> orderIds = orders.stream().map(EOrder::getId).collect(Collectors.toList());
        List<EOrderItem> allItems = orderItemService.lambdaQuery().in(EOrderItem::getOrderId, orderIds).list();
        Map<Long, List<EOrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(EOrderItem::getOrderId));

        List<OrderVO> records = orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            vo.setPayAmount(order.getTotalAmount() - order.getDiscountAmount());
            vo.setStatusText(order.getStatus() != null ? order.getStatus().getDesc() : null);
            vo.setReceiverPhone(maskPhone(order.getReceiverPhone()));
            vo.setReceiverAddr(maskAddr(order.getReceiverAddr()));

            List<EOrderItem> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());
            vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return R.ok(PageDTO.of(page, records));
    }

    @Override
    public R<Void> updateStatus(Long orderNo, Integer status) {
        Long shopId = UserContext.getShopId();
        if (shopId == null) throw new BizIllegalException("非商家账号");

        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null || !order.getShopId().equals(shopId)) {
            throw new BadRequestException("订单不存在");
        }

        OrderStatus newStatus = OrderStatus.of(status);
        // 商家只能改为发货或完成
        if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.COMPLETED) {
            throw new BadRequestException("商家只能发货或完成订单");
        }
        // 状态单向流转校验
        OrderStatus current = order.getStatus();
        if (newStatus == OrderStatus.SHIPPED && current != OrderStatus.PAID) {
            throw new BadRequestException("只能对已支付订单发货");
        }
        if (newStatus == OrderStatus.COMPLETED && current != OrderStatus.SHIPPED) {
            throw new BadRequestException("只能对已发货订单标记完成");
        }

        lambdaUpdate()
                .eq(EOrder::getOrderNo, orderNo)
                .set(EOrder::getStatus, newStatus)
                .update();
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> cancel(Long orderNo) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BizIllegalException("请先登录");
        //通过订单编号查询订单
        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BadRequestException("订单不存在");
        }
        //仅待支付订单可以取消，已支付需要走退款通道
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("仅待支付订单可以取消");
        }

        //通过乐观锁(状态为待支付的才可以更新)取消订单
        boolean cancelled = lambdaUpdate()
                .eq(EOrder::getOrderNo, orderNo)
                .eq(EOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(EOrder::getStatus, OrderStatus.CANCELLED)
                .set(EOrder::getCancelTime, LocalDateTime.now())
                .update();
        if (!cancelled) {
            throw new BadRequestException("订单状态已变更，请刷新后重试");
        }

        //回退优惠卷为未使用
        userCouponService.lambdaUpdate()
                .eq(EUserCoupon::getOrderNo, orderNo)
                .eq(EUserCoupon::getStatus, UserCouponStatus.USED)
                .set(EUserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(EUserCoupon::getUsedAt, null)
                .set(EUserCoupon::getOrderNo, null)
                .update();

        //查询订单商品明细项
        List<EOrderItem> items = orderItemService.lambdaQuery()
                .eq(EOrderItem::getOrderId, order.getId()).list();

        boolean isFlashSale = order.getFlashSaleOrderId() != null;

        if (isFlashSale) {
            // 秒杀订单：SKU 库存未被扣减（只扣了 Flash 库存），不在此恢复。
            // 通知 product-service 回补秒杀 Redis 库存（FLASH_STOCK_PREFIX / FLASH_USER_PREFIX）。
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            OrderCancelledMessage cancelledMsg = OrderCancelledMessage.builder()
                                    .orderNo(orderNo)
                                    .userId(userId)
                                    .build();
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, ORDER_CANCELLED_NOTIFY_KEY, cancelledMsg);
                            sendCancelNotification(orderNo, order);
                        }
                    });
        } else if (CollUtils.isNotEmpty(items)) {
            // 普通订单：Redis 回补 SKU 库存 + MQ 同步到 DB
            List<StockSyncBatchMessage.SkuItem> mergedItems = items.stream()
                    .collect(Collectors.groupingBy(EOrderItem::getSkuId,
                            LinkedHashMap::new, Collectors.summingInt(EOrderItem::getQuantity)))
                    .entrySet().stream()
                    .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisTemplate.executePipelined(new SessionCallback<Object>() {
                                @Override
                                public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                                    for (StockSyncBatchMessage.SkuItem item : mergedItems) {
                                        ops.opsForValue().increment(SKU_STOCK_PREFIX + item.getSkuId(),
                                                item.getQuantity());
                                    }
                                    return null;
                                }
                            });
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, STOCK_RESTORE_KEY,
                                    new StockSyncBatchMessage(IdUtil.fastSimpleUUID(), mergedItems));
                            sendCancelNotification(orderNo, order);
                        }
                    });
        } else {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendCancelNotification(orderNo, order);
                        }
                    });
        }

        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> payCallback(Long orderNo, String payNo, String payTime) {
        // 幂等：已支付直接返回成功
        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null) throw new BadRequestException("订单不存在");
        if (order.getStatus() == OrderStatus.PAID) {
            return R.ok();
        }
        // 乐观锁更新：仅待支付→已支付
        boolean updated = lambdaUpdate()
                .eq(EOrder::getOrderNo, orderNo)
                .eq(EOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(EOrder::getStatus, OrderStatus.PAID)
                .set(EOrder::getPayNo, payNo)
                .set(EOrder::getPayTime, LocalDateTime.parse(payTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .update();
        if (!updated) {
            log.warn("支付回调更新订单失败（状态已变更）: orderNo={}", orderNo);
            return R.error("订单状态已变更");
        }
        final Long flashSaleOrderId = order.getFlashSaleOrderId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_PAY_KEY, orderNo);
                        String payContent = "订单 #" + orderNo + " 已支付 ¥"
                                + String.format("%.2f", order.getTotalAmount() / 100.0) + "，请尽快发货";
                        OrderNotificationMessage msg = OrderNotificationMessage.builder()
                                .orderNo(orderNo)
                                .shopId(order.getShopId())
                                .type(NotificationType.NEW_ORDER)
                                .title("支付成功通知")
                                .content(payContent)
                                .build();
                        rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, msg);
                        // 秒杀订单：同步 EFlashSaleOrder 状态为 PAID，防止超时任务误回补库存
                        if (flashSaleOrderId != null) {
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_ORDER_PAID_KEY,
                                    FlashOrderPaidMessage.builder()
                                            .flashSaleOrderId(flashSaleOrderId)
                                            .build());
                        }
                    }
                });
        log.info("支付回调处理成功: orderNo={}, payNo={}", orderNo, payNo);
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> refundCallback(Long orderNo, Integer refundAmount) {
        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null) throw new BadRequestException("订单不存在");
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("仅已支付或已发货订单可退款");
        }

        boolean updated = lambdaUpdate()
                .eq(EOrder::getOrderNo, orderNo)
                .in(EOrder::getStatus, List.of(OrderStatus.PAID, OrderStatus.SHIPPED))
                .set(EOrder::getStatus, OrderStatus.REFUNDED)
                .update();
        if (!updated) {
            log.warn("退款回调更新订单失败（状态已变更）: orderNo={}", orderNo);
            return R.error("订单状态已变更");
        }

        // 退回优惠券
        userCouponService.lambdaUpdate()
                .eq(EUserCoupon::getOrderNo, orderNo)
                .eq(EUserCoupon::getStatus, UserCouponStatus.USED)
                .set(EUserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(EUserCoupon::getUsedAt, null)
                .set(EUserCoupon::getOrderNo, null)
                .update();

        boolean isFlashSale = order.getFlashSaleOrderId() != null;

        if (isFlashSale) {
            // 秒杀订单：SKU 库存未被扣减，通知 product-service 回补秒杀 Redis 库存
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            OrderCancelledMessage refundMsg = OrderCancelledMessage.builder()
                                    .orderNo(orderNo)
                                    .userId(order.getUserId())
                                    .build();
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, FLASH_SALE_REFUND_KEY, refundMsg);
                            sendRefundNotification(orderNo, order, refundAmount);
                        }
                    });
        } else {
            // 普通订单：恢复 SKU 库存
            List<EOrderItem> items = orderItemService.lambdaQuery()
                    .eq(EOrderItem::getOrderId, order.getId()).list();
            final List<StockSyncBatchMessage.SkuItem> mergedItems = CollUtils.isNotEmpty(items)
                    ? items.stream()
                        .collect(Collectors.groupingBy(EOrderItem::getSkuId,
                                LinkedHashMap::new, Collectors.summingInt(EOrderItem::getQuantity)))
                        .entrySet().stream()
                        .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                        .collect(Collectors.toList())
                    : Collections.emptyList();

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            if (!mergedItems.isEmpty()) {
                                redisTemplate.executePipelined(new SessionCallback<Object>() {
                                    @Override
                                    public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                                        for (StockSyncBatchMessage.SkuItem item : mergedItems) {
                                            ops.opsForValue().increment(SKU_STOCK_PREFIX + item.getSkuId(),
                                                    item.getQuantity());
                                        }
                                        return null;
                                    }
                                });
                                rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, STOCK_RESTORE_KEY,
                                        new StockSyncBatchMessage(IdUtil.fastSimpleUUID(), mergedItems));
                            }
                            sendRefundNotification(orderNo, order, refundAmount);
                        }
                    });
        }

        log.info("退款回调处理成功: orderNo={}, refundAmount={}", orderNo, refundAmount);
        return R.ok();
    }

    @Override
    @Transactional
    public R<Void> cancelByTimeout(Long orderNo) {
        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null) throw new BadRequestException("订单不存在");
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return R.ok();
        }

        boolean cancelled = lambdaUpdate()
                .eq(EOrder::getOrderNo, orderNo)
                .eq(EOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(EOrder::getStatus, OrderStatus.CANCELLED)
                .set(EOrder::getCancelTime, LocalDateTime.now())
                .update();
        if (!cancelled) return R.ok();

        // 退回优惠券
        userCouponService.lambdaUpdate()
                .eq(EUserCoupon::getOrderNo, orderNo)
                .eq(EUserCoupon::getStatus, UserCouponStatus.USED)
                .set(EUserCoupon::getStatus, UserCouponStatus.UNUSED)
                .set(EUserCoupon::getUsedAt, null)
                .set(EUserCoupon::getOrderNo, null)
                .update();

        boolean isFlashSale = order.getFlashSaleOrderId() != null;

        if (isFlashSale) {
            // 秒杀订单：库存由 product-service 的 FlashSaleTimeoutJob 处理，这里只同步取消 EOrder
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendCancelNotification(orderNo, order);
                        }
                    });
            log.info("秒杀超时同步取消 EOrder 完成: orderNo={}", orderNo);
        } else {
            // 普通订单：查询订单明细，恢复 Redis SKU 库存 + MQ 同步 DB
            List<EOrderItem> items = orderItemService.lambdaQuery()
                    .eq(EOrderItem::getOrderId, order.getId()).list();
            List<StockSyncBatchMessage.SkuItem> mergedItems = items.stream()
                    .collect(Collectors.groupingBy(EOrderItem::getSkuId,
                            LinkedHashMap::new, Collectors.summingInt(EOrderItem::getQuantity)))
                    .entrySet().stream()
                    .map(e -> new StockSyncBatchMessage.SkuItem(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            redisTemplate.executePipelined(new SessionCallback<Object>() {
                                @Override
                                @SuppressWarnings({"rawtypes", "unchecked"})
                                public Object execute(org.springframework.data.redis.core.RedisOperations ops) {
                                    for (StockSyncBatchMessage.SkuItem item : mergedItems) {
                                        ops.opsForValue().increment(SKU_STOCK_PREFIX + item.getSkuId(),
                                                item.getQuantity());
                                    }
                                    return null;
                                }
                            });
                            rabbitMqHelper.sendAsync(PRODUCT_EXCHANGE, STOCK_RESTORE_KEY,
                                    new StockSyncBatchMessage(IdUtil.fastSimpleUUID(), mergedItems));
                            sendCancelNotification(orderNo, order);
                        }
                    });
            log.info("普通订单超时取消完成: orderNo={}", orderNo);
        }

        return R.ok();
    }

    @Override
    public OrderStatisticsDTO getStatistics() {
        Long totalOrders = lambdaQuery().count();
        Long totalSales = baseMapper.sumCompletedSales();
        OrderStatisticsDTO dto = new OrderStatisticsDTO();
        dto.setTotalOrders(totalOrders);
        dto.setTotalSales(totalSales != null ? totalSales : 0L);
        return dto;
    }

    @Override
    public OrderBasicDTO getOrderBasic(Long orderNo) {
        EOrder order = lambdaQuery().eq(EOrder::getOrderNo, orderNo).one();
        if (order == null) return null;
        OrderBasicDTO dto = new OrderBasicDTO();
        BeanUtils.copyProperties(order, dto);
        dto.setFlashSaleOrderId(order.getFlashSaleOrderId());
        dto.setStatus(order.getStatus().getValue());
        dto.setSubject("订单" + order.getOrderNo());
        return dto;
    }

    // ======================== helpers ========================

    private void sendCancelNotification(Long orderNo, EOrder order) {
        int cancelAmount = order.getTotalAmount() != null ? order.getTotalAmount() : 0;
        OrderNotificationMessage notifyMsg = OrderNotificationMessage.builder()
                .orderNo(orderNo)
                .shopId(order.getShopId())
                .type(NotificationType.NEW_ORDER)
                .title("订单已取消")
                .content("订单 #" + orderNo + " 已取消，金额 ¥"
                        + String.format("%.2f", cancelAmount / 100.0))
                .build();
        rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, notifyMsg);
    }

    private void sendRefundNotification(Long orderNo, EOrder order, Integer refundAmount) {
        int amount = refundAmount != null ? refundAmount : 0;
        OrderNotificationMessage notifyMsg = OrderNotificationMessage.builder()
                .orderNo(orderNo)
                .shopId(order.getShopId())
                .type(NotificationType.NEW_ORDER)
                .title("订单已退款")
                .content("订单 #" + orderNo + " 已退款 ¥"
                        + String.format("%.2f", amount / 100.0))
                .build();
        rabbitMqHelper.sendAsync(ORDER_EXCHANGE, ORDER_NOTIFY_KEY, notifyMsg);
    }

    private OrderItemVO toOrderItemVO(EOrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskAddr(String addr) {
        if (addr == null || addr.length() <= 6) return addr;
        return addr.substring(0, 6) + "...";
    }
}