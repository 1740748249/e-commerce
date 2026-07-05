package com.ecommerce.notification.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.notification.domain.dto.CreateNotificationDTO;
import com.ecommerce.notification.domain.vo.NotificationVO;
import com.ecommerce.notification.service.IENotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/notifications")
@Slf4j
@RequiredArgsConstructor
public class ENotificationController {

    private final IENotificationService notificationService;

    @GetMapping("/shop")
    public R<PageDTO<NotificationVO>> getShopNotifications(PageQuery query) {
        return notificationService.getShopNotifications(query);
    }

    @GetMapping("/unread-count")
    public R<Integer> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @PutMapping("/{id}/read")
    public R<Void> markAsRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    public R<Void> markAllAsRead() {
        return notificationService.markAllAsRead();
    }

    /**
     * 管理员发送系统通知或促销通知（type=1 或 2）。
     * 权限应在 Gateway 层限制 role=2。
     */
    @PostMapping
    public R<Void> sendNotification(@Valid @RequestBody CreateNotificationDTO dto) {
        return notificationService.sendAdminNotification(dto);
    }

    /**
     * 管理员发送全站广播，实时推送给所有在线的商家。
     */
    @PostMapping("/broadcast")
    public R<Void> sendBroadcast(@RequestParam String title, @RequestParam String content) {
        return notificationService.sendBroadcast(title, content);
    }
}
