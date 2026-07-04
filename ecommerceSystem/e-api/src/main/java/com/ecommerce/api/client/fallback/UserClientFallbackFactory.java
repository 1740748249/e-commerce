package com.ecommerce.api.client.fallback;

import com.ecommerce.api.client.UserClient;
import com.ecommerce.api.dto.AddressDTO;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.api.enums.UserRole;
import com.ecommerce.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public R<Void> updateUserRole(Long userId, UserRole role) {
                log.error("Feign 调用 user-service 更新用户角色失败: userId={}, role={}", userId, role, cause);
                return R.error("用户服务暂不可用，角色更新失败");
            }

            @Override
            public R<List<UserBriefDTO>> getUsersByIds(List<Long> ids) {
                log.error("Feign 调用 user-service 批量查询用户失败: ids={}", ids, cause);
                return R.ok(Collections.emptyList());
            }

            @Override
            public R<AddressDTO> getAddressById(Long id) {
                log.error("Feign 调用 user-service 根据id查询地址失败: id={}", id, cause);
                return R.error("用户服务暂不可用，地址查询失败");
            }
        };
    }
}
