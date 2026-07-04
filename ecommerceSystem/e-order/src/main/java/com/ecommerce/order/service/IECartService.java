package com.ecommerce.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.common.domain.R;
import com.ecommerce.order.domain.dto.CartAddDTO;
import com.ecommerce.order.domain.po.ECart;
import com.ecommerce.order.domain.vo.CartVO;

import java.util.List;

public interface IECartService extends IService<ECart> {

    R<List<CartVO>> getCartList();

    R<Void> add(CartAddDTO dto);

    R<Void> update(Long cartItemId, Integer quantity);

    R<Void> remove(Long cartItemId);

    R<Void> clear();
}
