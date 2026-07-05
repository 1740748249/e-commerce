package com.ecommerce.notification.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.notification.domain.po.ENotificationRead;
import com.ecommerce.notification.mapper.ENotificationReadMapper;
import com.ecommerce.notification.service.IENotificationReadService;
import org.springframework.stereotype.Service;

@Service
public class ENotificationReadServiceImpl extends ServiceImpl<ENotificationReadMapper, ENotificationRead> implements IENotificationReadService {
}
