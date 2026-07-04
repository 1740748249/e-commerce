package com.ecommerce.user.service.impl;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.user.config.JwtProperties;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.dto.PasswordDTO;
import com.ecommerce.user.domain.dto.RegisterDTO;
import com.ecommerce.user.domain.dto.UpdateUserDTO;
import com.ecommerce.user.domain.po.EUser;
import com.ecommerce.user.domain.vo.AddressVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.mapper.EUserMapper;
import com.ecommerce.user.service.IEUserAddressService;
import com.ecommerce.user.service.IEUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EUserServiceImpl extends ServiceImpl<EUserMapper, EUser> implements IEUserService {
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final IEUserAddressService addressService;

    @Override
    public R<UserVO> register(RegisterDTO dto) {

        // check username duplicate
        EUser exist = lambdaQuery().eq(EUser::getUsername, dto.getUsername()).one();
        if (exist != null) {
            throw new BadRequestException("用户名已被注册");
        }
        if (dto.getPhone() == null || dto.getPhone().isEmpty()) {
            throw new BadRequestException("手机号不能为空");
        }
        exist = lambdaQuery().eq(EUser::getPhone, dto.getPhone()).one();
        if (exist != null) {
            throw new BadRequestException("手机号已被注册");
        }

        EUser eUser = BeanUtils.copyBean(dto, EUser.class);
        eUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        eUser.setRole(UserRole.USER);
        eUser.setStatus(UserStatus.ACTIVE);
        save(eUser);
        UserVO vo = BeanUtils.copyBean(eUser, UserVO.class);
        log.info("用户注册成功：{}", vo);
        return R.ok(vo);
    }

    @Override
    public R<LoginVO> login(LoginDTO dto) {
        EUser eUser = lambdaQuery()
                .eq(EUser::getUsername, dto.getUsername())
                .eq(EUser::getStatus, UserStatus.ACTIVE)
                .one();
        if (eUser == null) {
            throw new BadRequestException("用户不存在或账号违规被禁用");
        }
        //检查密码是否正确
        if (!passwordEncoder.matches(dto.getPassword(), eUser.getPassword())) {
            throw new BadRequestException("密码错误");
        }
        UserVO userVO = BeanUtils.copyBean(eUser, UserVO.class);
        userVO.setLastLoginTime(LocalDateTime.now());
        eUser.setLastLoginTime(LocalDateTime.now());
        updateById(eUser);
        //生成token
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtTool.createToken(eUser.getId(), eUser.getRole().getValue(), Duration.parse(jwtProperties.getTokenTTL())));
        loginVO.setUser(userVO);
        log.info("用户登录成功：{}", userVO);
        return R.ok(loginVO);
    }

    @Override
    public R<UserVO> getUserInfo() {
        Long userId = UserContext.getUserId();
        EUser eUser = getById(userId);
        if (eUser == null) {
            throw new BadRequestException("用户不存在");
        }
        UserVO userVO = BeanUtils.copyBean(eUser, UserVO.class);
        try {
            List<AddressVO> addresses = addressService.getUserAddresses().getData();
            if (addresses != null) {
                AddressVO defaultAddr = addresses.stream()
                        .filter(a -> a.getIsDefault() != null && a.getIsDefault() == 1)
                        .findFirst()
                        .orElse(addresses.isEmpty() ? null : addresses.get(0));
                if (defaultAddr != null) {
                    userVO.setAddr(defaultAddr.getProvince() + defaultAddr.getCity()
                            + defaultAddr.getDistrict() + defaultAddr.getDetail());
                }
            }
        } catch (Exception e) {
            log.warn("获取用户默认地址失败: userId={}", userId, e);
        }
        return R.ok(userVO);
    }

    @Override
    public R<List<AddressVO>> getUserAddresses() {
        return addressService.getUserAddresses();
    }

    @Override
    public R<Void> updateUserInfo(UpdateUserDTO dto) {
        //获取用户id
        Long userId = UserContext.getUserId();
        EUser eUser = BeanUtils.copyBean(dto, EUser.class);
        eUser.setId(userId);
        boolean success = updateById(eUser);
        if(!success){
            throw new BadRequestException("更新用户信息失败");
        }
        return R.ok();
    }

    @Override
    public R<Void> updatePassword(PasswordDTO dto) {
        Long userId = UserContext.getUserId();
        EUser user = getById(userId);
        //检查旧密码是否正确
        if (!new BCryptPasswordEncoder().matches(dto.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("旧密码错误");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getNewPassword()));
        boolean success = updateById(user);
        if(!success){
            throw new BadRequestException("更新密码失败");
        }
        return R.ok();
    }
}
