package com.ecommerce.user.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

// LoginDTO.java
  @Data
  public class LoginDTO implements Serializable {
      @NotBlank(message = "用户名不能为空")
      private String username;

      @NotBlank(message = "密码不能为空")
      private String password;
  }