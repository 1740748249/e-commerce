package com.ecommerce.notification.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.ecommerce.notification.domain.vo.NotificationVO;
import com.ecommerce.notification.enums.IsReadStatus;
import com.ecommerce.notification.mapper.ENotificationMapper;
import com.ecommerce.notification.service.IENotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

import static com.ecommerce.notification.constants.RedisConstants.DEDUP_TTL_SECONDS;
import static com.ecommerce.notification.constants.RedisConstants.NOTIFY_DEDUP_KEY;
import static com.ecommerce.notification.constants.RedisConstants.SHOP_UNREAD_KEY;
import static com.ecommerce.notification.constants.RedisConstants.UNREAD_TTL_SECONDS;

@Service
@Slf4j
@RequiredArgsConstructor
public class ENotificationServiceImpl extends ServiceImpl<ENotificationMapper, ENotification> implements IENotificationService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public R<PageDTO<NotificationVO>> getShopNotifications(PageQuery query) {
        Long shopId = getCurrentShopId();
        Page<ENotification> page = lambdaQuery()
                .eq(ENotification::getShopId, shopId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<ENotification> records = page.getRecords();
        if(CollUtils.isEmpty(records)){
            return R.ok(PageDTO.empty(page));
        }
        List<NotificationVO> voList = BeanUtils.copyList(records, NotificationVO.class);
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

        long count = lambdaQuery()
                .eq(ENotification::getShopId, shopId)
                .eq(ENotification::getIsRead, IsReadStatus.UNREAD)
                .count();
        int unread = (int) count;
        redisTemplate.opsForValue().set(key, String.valueOf(unread), Duration.ofSeconds(UNREAD_TTL_SECONDS));
        return R.ok(unread);
    }

    @Override
    public R<Void> markAsRead(Long id) {
        Long shopId = getCurrentShopId();
        ENotification notification = getById(id);
        if (notification == null || !shopId.equals(notification.getShopId())) {
            throw new BizIllegalException("通知不存在");
        }
        if (notification.getIsRead() == IsReadStatus.READ) {
            return R.ok();
        }
        boolean updated = lambdaUpdate()
                .eq(ENotification::getId, id)
                .eq(ENotification::getIsRead, IsReadStatus.UNREAD)
                .set(ENotification::getIsRead, IsReadStatus.READ)
                .update();
        if (updated) {
            decrUnreadCache(shopId);
        }
        return R.ok();
    }

    @Override
    public R<Void> markAllAsRead() {
        Long shopId = getCurrentShopId();
        lambdaUpdate()
                .eq(ENotification::getShopId, shopId)
                .eq(ENotification::getIsRead, IsReadStatus.UNREAD)
                .set(ENotification::getIsRead, IsReadStatus.READ)
                .update();
        redisTemplate.delete(String.format(SHOP_UNREAD_KEY, shopId));
        return R.ok();
    }

    @Override
    public void createNotification(OrderNotificationMessage msg) {
        if (msg.getType() == null || msg.getShopId() == null) {
            log.warn("通知消息字段缺失，已跳过: orderNo={}", msg.getOrderNo());
            return;
        }

        // SET NX 原子去重：返回 true 表示首次处理，false 表示重复消息
        String dedupKey = String.format(NOTIFY_DEDUP_KEY, msg.getOrderNo(), msg.getType().getValue());
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(DEDUP_TTL_SECONDS));
        if (acquired == null || !acquired) {
            log.info("通知已存在（SET NX 拒绝），跳过: orderNo={}, type={}", msg.getOrderNo(), msg.getType());
            return;
        }

        try {
            ENotification notification = BeanUtils.copyBean(msg, ENotification.class, (src, target) -> {
                target.setType(src.getType());
                target.setOrderId(src.getOrderNo());
                target.setIsRead(IsReadStatus.UNREAD);
            });
            save(notification);
        } catch (Exception e) {
            log.error("通知创建失败: orderNo={}, type={}", msg.getOrderNo(), msg.getType(), e);
            redisTemplate.delete(dedupKey);
            throw e;
        }

        String unreadKey = String.format(SHOP_UNREAD_KEY, msg.getShopId());
        redisTemplate.opsForValue().increment(unreadKey);
        redisTemplate.expire(unreadKey, Duration.ofSeconds(UNREAD_TTL_SECONDS));

        log.info("通知已创建: shopId={}, orderNo={}, type={}", msg.getShopId(), msg.getOrderNo(), msg.getType());
    }

    @Override
    public R<Void> sendAdminNotification(CreateNotificationDTO dto) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BizIllegalException("仅管理员可发送通知");
        }
        if (dto.getType() != NotificationType.SYSTEM && dto.getType() != NotificationType.PROMOTION) {
            throw new BizIllegalException("仅允许发送系统通知或促销活动");
        }

        ENotification notification = BeanUtils.copyBean(dto, ENotification.class, (src, target) -> {
            target.setType(src.getType());
            target.setIsRead(IsReadStatus.UNREAD);
        });
        save(notification);

        String unreadKey = String.format(SHOP_UNREAD_KEY, dto.getShopId());
        redisTemplate.opsForValue().increment(unreadKey);
        redisTemplate.expire(unreadKey, Duration.ofSeconds(UNREAD_TTL_SECONDS));

        return R.ok();
    }

    // ======================== helpers ========================

    private Long getCurrentShopId() {
        Long shopId = UserContext.getShopId();
        if (shopId == null) {
            throw new BizIllegalException("仅商家可操作");
        }
        return shopId;
    }

    private void decrUnreadCache(Long shopId) {
        String key = String.format(SHOP_UNREAD_KEY, shopId);
        Long val = redisTemplate.opsForValue().decrement(key);
        if (val != null && val < 0) {
            redisTemplate.delete(key);
        }
    }
}
