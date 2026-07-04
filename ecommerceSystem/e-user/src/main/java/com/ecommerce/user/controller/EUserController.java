package com.ecommerce.user.controller;


import com.ecommerce.common.domain.R;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.dto.PasswordDTO;
import com.ecommerce.user.domain.dto.RegisterDTO;
import com.ecommerce.user.domain.dto.UpdateUserDTO;
import com.ecommerce.user.domain.vo.AddressVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;
import com.ecommerce.user.service.IEUserService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@RestController
@RequestMapping("/users")
@Slf4j
@Api(tags = "用户相关接口")
@RequiredArgsConstructor
public class EUserController {
    private final IEUserService userService;

  /**
   *  注册
   * @param dto
   * @return
   */
  @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterDTO dto){
      return userService.register(dto);
  }

  /**
   * 登录
   * @param dto
   * @return
   */
  @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto){
      return userService.login(dto);
  }

  /**
   * 获取当前用户信息
   * @return
   */
  @GetMapping("/me")
  public R<UserVO> getUserInfo(){
      return userService.getUserInfo();
  }

  @PutMapping("/me")
  public R<Void> updateUserInfo(@Valid @RequestBody UpdateUserDTO dto){
      return userService.updateUserInfo(dto);
  }
  /**
   * 获取当前用户收货地址列表
   * @return
   */
  @GetMapping("/addresses")
  public R<List<AddressVO>> getUserAddresses(){
      return userService.getUserAddresses();
  }

  @PutMapping("/password")
  public R<Void> updatePassword(@Valid @RequestBody PasswordDTO dto){
      return userService.updatePassword(dto);
  }

}
