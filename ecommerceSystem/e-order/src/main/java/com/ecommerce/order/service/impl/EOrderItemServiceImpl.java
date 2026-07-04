package com.ecommerce.order.service.impl;

import com.ecommerce.order.domain.po.EOrderItem;
import com.ecommerce.order.mapper.EOrderItemMapper;
import com.ecommerce.order.service.IEOrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单商品明细表（快照） 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
@Service
public class EOrderItemServiceImpl extends ServiceImpl<EOrderItemMapper, EOrderItem> implements IEOrderItemService {

}
