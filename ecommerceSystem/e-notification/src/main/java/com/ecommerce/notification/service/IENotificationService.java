package com.ecommerce.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.api.message.OrderNotificationMessage;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.notification.domain.dto.CreateNotificationDTO;
import com.ecommerce.notification.domain.po.ENotification;
import com.ecommerce.notification.domain.vo.NotificationVO;

public interface IENotificationService extends IService<ENotification> {

    R<PageDTO<NotificationVO>> getShopNotifications(PageQuery query);

    R<Integer> getUnreadCount();

    R<Void> markAsRead(Long id);

    R<Void> markAllAsRead();

    void createNotification(OrderNotificationMessage msg);

    R<Void> sendAdminNotification(CreateNotificationDTO dto);
}
