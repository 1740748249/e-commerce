package com.ecommerce.user.service;

import com.ecommerce.common.domain.R;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.dto.PasswordDTO;
import com.ecommerce.user.domain.dto.RegisterDTO;
import com.ecommerce.user.domain.dto.UpdateUserDTO;
import com.ecommerce.user.domain.po.EUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.user.domain.vo.AddressVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;

import java.util.List;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
public interface IEUserService extends IService<EUser> {

    R<UserVO> register(RegisterDTO dto);

    R<LoginVO> login(LoginDTO dto);

    R<UserVO> getUserInfo();


    R<List<AddressVO>> getUserAddresses();

    R<Void> updateUserInfo(UpdateUserDTO dto);


    R<Void> updatePassword(PasswordDTO dto);
}
