package com.ecommerce.product.service.impl;

import com.ecommerce.product.domain.po.EFlashSaleOrder;
import com.ecommerce.product.mapper.EFlashSaleOrderMapper;
import com.ecommerce.product.service.IEFlashSaleOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀订单记录表（防刷） 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Service
public class EFlashSaleOrderServiceImpl extends ServiceImpl<EFlashSaleOrderMapper, EFlashSaleOrder> implements IEFlashSaleOrderService {

}
