package com.ecommerce.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserBriefDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String phone;
}
