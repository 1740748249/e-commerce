package com.ecommerce.common.interceptor;

import com.ecommerce.common.utils.StringUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.common.utils.UserContext.UserInfo;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userInfo = request.getHeader("user-info");
        if (StringUtils.isNotBlank(userInfo)) {
            Long userId = Long.valueOf(userInfo);
            Long shopId = null;
            Integer role = null;
            String shopIdHeader = request.getHeader("X-Shop-Id");
            if (StringUtils.isNotBlank(shopIdHeader)) {
                shopId = Long.valueOf(shopIdHeader);
            }
            String roleHeader = request.getHeader("X-User-Role");
            if (StringUtils.isNotBlank(roleHeader)) {
                role = Integer.valueOf(roleHeader);
            }
            UserContext.set(new UserInfo(userId, shopId, role));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.remove();
    }
}
