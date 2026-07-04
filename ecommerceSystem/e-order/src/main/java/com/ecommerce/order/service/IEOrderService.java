package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.api.dto.OrderBasicDTO;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.order.domain.dto.BuyNowDTO;
import com.ecommerce.order.domain.dto.CreateOrderDTO;
import com.ecommerce.order.domain.dto.OrderPreviewDTO;
import com.ecommerce.order.domain.po.EOrder;
import com.ecommerce.order.query.OrderPageQuery;
import com.ecommerce.order.domain.vo.OrderDetailVO;
import com.ecommerce.order.domain.vo.OrderPreviewVO;
import com.ecommerce.order.domain.vo.OrderVO;

public interface IEOrderService extends IService<EOrder> {

    R<Long> create(CreateOrderDTO dto);

    R<Long> buyNow(BuyNowDTO dto);

    R<OrderPreviewVO> preview(OrderPreviewDTO dto);

    R<PageDTO<OrderVO>> myOrders(OrderPageQuery query);

    R<OrderDetailVO> detail(Long orderNo);

    R<PageDTO<OrderVO>> shopOrders(OrderPageQuery query);

    R<Void> updateStatus(Long orderNo, Integer status);

    R<Void> cancel(Long orderNo);

    /** 秒杀超时取消（系统触发，无需用户鉴权，不重复通知 product-service） */
    R<Void> cancelByTimeout(Long orderNo);

    R<Void> payCallback(Long orderNo, String payNo, String payTime);

    R<Void> refundCallback(Long orderNo, Integer refundAmount);

    OrderBasicDTO getOrderBasic(Long orderNo);

    OrderStatisticsDTO getStatistics();
}
