package com.ecommerce.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

public class UserContext {
    @Data
    @AllArgsConstructor
    public static class UserInfo{
        private Long userId;
        private Long shopId;
        private Integer role;
    }
    private static final ThreadLocal<UserInfo> TL = new ThreadLocal<>();

    /**
     * 保存用户信息
     * @param info 用户id
     */
    public static void set(UserInfo info){
        TL.set(info);
    }

    /**
     * 获取用户
     * @return 用户id
     */
    public static Long getUserId(){
        UserInfo info = TL.get();
        return info != null ? info.getUserId() : null;
    }

    public static Long getShopId(){
        UserInfo info = TL.get();
        return info != null ? info.getShopId() : null;
    }

    public static Integer getRole(){
        UserInfo info = TL.get();
        return info != null ? info.getRole() : null;
    }

    /**
     * 移除用户信息
     */
    public static void remove(){
        TL.remove();
    }
}
