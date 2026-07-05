package com.ecommerce.notification.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.enums.NotificationType;
import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.notification.domain.dto.CreateNotificationDTO;
import com.ecommerce.notification.domain.po.ENotification;
import com.ecommerce.notification.domain.po.ENotificationRead;
import com.ecommerce.notification.domain.vo.NotificationVO;
import com.ecommerce.notification.enums.IsReadStatus;
import com.ecommerce.notification.mapper.ENotificationMapper;
import com.ecommerce.notification.service.IENotificationReadService;
import com.ecommerce.notification.service.IENotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ecommerce.notification.constants.RedisConstants.ADMIN_DEDUP_TTL_SECONDS;
import static com.ecommerce.notification.constants.RedisConstants.BROADCAST_DEDUP_TTL_SECONDS;
import static com.ecommerce.notification.constants.RedisConstants.DEDUP_TTL_SECONDS;
import static com.ecommerce.notification.constants.RedisConstants.NOTIFY_ADMIN_DEDUP_KEY;
import static com.ecommerce.notification.constants.RedisConstants.NOTIFY_BROADCAST_DEDUP_KEY;
import static com.ecommerce.notification.constants.RedisConstants.NOTIFY_DEDUP_KEY;
import static com.ecommerce.notification.constants.RedisConstants.SHOP_UNREAD_KEY;
import static com.ecommerce.notification.constants.RedisConstants.UNREAD_TTL_SECONDS;

@Service
@Slf4j
@RequiredArgsConstructor
public class ENotificationServiceImpl extends ServiceImpl<ENotificationMapper, ENotification> implements IENotificationService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProductClient productClient;
    private final IENotificationReadService readService;
    private final DefaultRedisScript<Long> incrAndExpireScript;
    private final DefaultRedisScript<Long> decrUnreadScript;
    private final DefaultRedisScript<Long> batchIncrAndExpireScript;

    @Override
    public R<PageDTO<NotificationVO>> getShopNotifications(PageQuery query) {
        Long shopId = getCurrentShopId();
        Page<ENotification> page = lambdaQuery()
                .and(w -> w.eq(ENotification::getShopId, shopId).or().eq(ENotification::getShopId, 0L))
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ENotification> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return R.ok(PageDTO.empty(page));
        }

        // 查询该店铺已读了哪些通知
        List<Long> notifIds = records.stream().map(ENotification::getId).collect(Collectors.toList());
        Set<Long> readIds = readService.lambdaQuery()
                .eq(ENotificationRead::getShopId, shopId)
                .in(ENotificationRead::getNotificationId, notifIds)
                .list()
                .stream()
                .map(ENotificationRead::getNotificationId)
                .collect(Collectors.toSet());

        List<NotificationVO> voList = BeanUtils.copyList(records, NotificationVO.class);
        for (NotificationVO vo : voList) {
            vo.setIsRead(readIds.contains(vo.getId()) ? IsReadStatus.READ : IsReadStatus.UNREAD);
        }
        return R.ok(PageDTO.of(page, voList));
    }

    @Override
    public R<Integer> getUnreadCount() {
        Long shopId = getCurrentShopId();
        String key = String.format(SHOP_UNREAD_KEY, shopId);

        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return R.ok(Integer.parseInt(cached));
            } catch (NumberFormatException ignored) {
                redisTemplate.delete(key);
            }
        }

        // 该店铺能看到的通知总数（自己的 + 广播）
        long totalCount = lambdaQuery()
                .and(w -> w.eq(ENotification::getShopId, shopId).or().eq(ENotification::getShopId, 0L))
                .count();
        // 减去该店铺已读的
        long readCount = readService.lambdaQuery()
                .eq(ENotificationRead::getShopId, shopId)
                .count();
        int unread = (int) Math.max(0, totalCount - readCount);
        redisTemplate.opsForValue().set(key, String.valueOf(unread), Duration.ofSeconds(UNREAD_TTL_SECONDS));
        return R.ok(unread);
    }

    @Override
    public R<Void> markAsRead(Long id) {
        Long shopId = getCurrentShopId();
        ENotification notification = getById(id);
        if (notification == null || (!shopId.equals(notification.getShopId()) && notification.getShopId() != 0L)) {
            throw new BizIllegalException("通知不存在");
        }

        ENotificationRead record = new ENotificationRead();
        record.setNotificationId(id);
        record.setShopId(shopId);
        record.setIsRead(IsReadStatus.READ);
        record.setReadTime(LocalDateTime.now());
        try {
            readService.save(record);
            decrUnreadCache(shopId);
        } catch (DuplicateKeyException e) {
            // 已读，忽略
        }
        return R.ok();
    }

    @Override
    public R<Void> markAllAsRead() {
        Long shopId = getCurrentShopId();

        // 该店铺能看到的全部通知 ID
        List<Long> notifIds = lambdaQuery()
                .and(w -> w.eq(ENotification::getShopId, shopId).or().eq(ENotification::getShopId, 0L))
                .list()
                .stream()
                .map(ENotification::getId)
                .collect(Collectors.toList());

        if (!notifIds.isEmpty()) {
            // 先查出该店铺已有哪些已读记录，避免重复插入
            Set<Long> alreadyRead = readService.lambdaQuery()
                    .eq(ENotificationRead::getShopId, shopId)
                    .in(ENotificationRead::getNotificationId, notifIds)
                    .list()
                    .stream()
                    .map(ENotificationRead::getNotificationId)
                    .collect(Collectors.toSet());

            LocalDateTime now = LocalDateTime.now();
            List<ENotificationRead> batch = notifIds.stream()
                    .filter(id -> !alreadyRead.contains(id))
                    .map(id -> {
                        ENotificationRead r = new ENotificationRead();
                        r.setNotificationId(id);
                        r.setShopId(shopId);
                        r.setIsRead(IsReadStatus.READ);
                        r.setReadTime(now);
                        return r;
                    })
                    .collect(Collectors.toList());

            if (!batch.isEmpty()) {
                readService.saveBatch(batch);
            }
        }

        redisTemplate.delete(String.format(SHOP_UNREAD_KEY, shopId));
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(OrderNotificationMessage msg) {
        if (msg.getType() == null || msg.getShopId() == null) {
            log.warn("通知消息字段缺失，已跳过: orderNo={}", msg.getOrderNo());
            return;
        }

        String dedupKey = String.format(NOTIFY_DEDUP_KEY, msg.getOrderNo(), msg.getType().getValue());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(DEDUP_TTL_SECONDS));
        if (acquired == null || !acquired) {
            log.info("通知已存在（SET NX 拒绝），跳过: orderNo={}, type={}", msg.getOrderNo(), msg.getType());
            return;
        }

        ENotification notification = BeanUtils.copyBean(msg, ENotification.class, (src, target) -> {
            target.setType(src.getType());
            target.setOrderId(src.getOrderNo());
        });

        try {
            save(notification);

            String unreadKey = String.format(SHOP_UNREAD_KEY, msg.getShopId());
            redisTemplate.execute(incrAndExpireScript,
                    Collections.singletonList(unreadKey), String.valueOf(UNREAD_TTL_SECONDS));
        } catch (Exception e) {
            log.error("通知创建失败: orderNo={}, type={}", msg.getOrderNo(), msg.getType(), e);
            redisTemplate.delete(dedupKey);
            throw e;
        }

        log.info("通知已创建: shopId={}, orderNo={}, type={}", msg.getShopId(), msg.getOrderNo(), msg.getType());
        pushToShop(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> sendAdminNotification(CreateNotificationDTO dto) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BizIllegalException("仅管理员可发送通知");
        }
        if (dto.getShopId() == null) {
            throw new BizIllegalException("店铺ID不能为空");
        }
        if (dto.getType() != NotificationType.SYSTEM && dto.getType() != NotificationType.PROMOTION) {
            throw new BizIllegalException("仅允许发送系统通知或促销活动");
        }

        String dedupKey = String.format(NOTIFY_ADMIN_DEDUP_KEY,
                dto.getShopId(), dto.getType().getValue(), dto.getTitle().hashCode());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(ADMIN_DEDUP_TTL_SECONDS));
        if (acquired == null || !acquired) {
            throw new BizIllegalException("2小时内已向该店铺发送过相同类型的同标题通知，请勿重复发送");
        }

        ENotification notification = BeanUtils.copyBean(dto, ENotification.class, (src, target) -> {
            target.setType(src.getType());
        });

        try {
            save(notification);

            String unreadKey = String.format(SHOP_UNREAD_KEY, dto.getShopId());
            redisTemplate.execute(incrAndExpireScript,
                    Collections.singletonList(unreadKey), String.valueOf(UNREAD_TTL_SECONDS));
        } catch (Exception e) {
            log.error("管理员通知保存失败: shopId={}, type={}", dto.getShopId(), dto.getType(), e);
            redisTemplate.delete(dedupKey);
            throw e;
        }

        pushToShop(notification);
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> sendBroadcast(String title, String content) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BizIllegalException("仅管理员可发送广播");
        }

        String dedupKey = String.format(NOTIFY_BROADCAST_DEDUP_KEY, title.hashCode());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(BROADCAST_DEDUP_TTL_SECONDS));
        if (acquired == null || !acquired) {
            throw new BizIllegalException("1小时内已发送过相同标题的广播，请勿重复发送");
        }

        ENotification notification = new ENotification();
        notification.setShopId(0L);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle(title);
        notification.setContent(content);

        try {
            save(notification);
            incrementAllShopUnread();
        } catch (Exception e) {
            log.error("广播保存失败: title={}", title, e);
            redisTemplate.delete(dedupKey);
            throw e;
        }

        NotificationVO vo = BeanUtils.copyBean(notification, NotificationVO.class);
        messagingTemplate.convertAndSend("/topic/broadcast", vo);
        log.info("广播推送并落库成功: title={}", title);
        return R.ok();
    }

    // ======================== helpers ========================

    private void pushToShop(ENotification notification) {
        try {
            NotificationVO vo = BeanUtils.copyBean(notification, NotificationVO.class);
            messagingTemplate.convertAndSend("/topic/shop/" + notification.getShopId(), vo);
        } catch (Exception e) {
            log.error("WebSocket 推送失败: shopId={}", notification.getShopId(), e);
        }
    }

    private Long getCurrentShopId() {
        Long shopId = UserContext.getShopId();
        if (shopId == null) {
            throw new BizIllegalException("仅商家可操作");
        }
        return shopId;
    }

    private void decrUnreadCache(Long shopId) {
        String key = String.format(SHOP_UNREAD_KEY, shopId);
        redisTemplate.execute(decrUnreadScript,
                Collections.singletonList(key));
    }

    private void incrementAllShopUnread() {
        List<Long> shopIds = getShopIdsForBroadcast();
        if (shopIds.isEmpty()) return;
        List<String> keys = shopIds.stream()
                .map(id -> String.format(SHOP_UNREAD_KEY, id))
                .collect(Collectors.toList());
        redisTemplate.execute(batchIncrAndExpireScript,
                keys, String.valueOf(UNREAD_TTL_SECONDS));
        log.info("已递增 {} 个店铺的未读计数", shopIds.size());
    }

    private List<Long> getShopIdsForBroadcast() {
        Set<Object> hashKeys = redisTemplate.opsForHash().keys("product:shop:all");
        if (hashKeys != null && !hashKeys.isEmpty()) {
            return hashKeys.stream()
                    .map(Object::toString)
                    .filter(k -> !"__HASH_EMPTY__".equals(k))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        try {
            R<List<Long>> r = productClient.getAllApprovedShopIds();
            return (r != null && r.getData() != null) ? r.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Feign 获取店铺ID列表失败", e);
            return Collections.emptyList();
        }
    }
}
