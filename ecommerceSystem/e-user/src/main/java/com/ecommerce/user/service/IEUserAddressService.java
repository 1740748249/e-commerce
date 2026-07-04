package com.ecommerce.user.service;

import com.ecommerce.common.domain.R;
import com.ecommerce.user.domain.dto.AddressDTO;
import com.ecommerce.user.domain.po.EUserAddress;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.user.domain.vo.AddressVO;

import java.util.List;

/**
 * <p>
 * 用户收货地址表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
public interface IEUserAddressService extends IService<EUserAddress> {

    R<List<AddressVO>> getUserAddresses();

    R<AddressVO> getAddressById(Long id);

    R<Void> addUserAddress(AddressDTO dto);

    R<Void> updateUserAddress(Long id, AddressDTO dto);

    R<Void> deleteUserAddress(Long id);
}
