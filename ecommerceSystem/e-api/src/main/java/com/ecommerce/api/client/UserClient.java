package com.ecommerce.api.client;

import com.ecommerce.api.client.fallback.UserClientFallbackFactory;
import com.ecommerce.api.dto.AddressDTO;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.api.enums.UserRole;
import com.ecommerce.common.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
    name = "user-service",
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @PutMapping("/admin/users/{id}/role")
    R<Void> updateUserRole(@PathVariable("id") Long userId, @RequestParam("role") UserRole role);

    @GetMapping("/admin/users/batch")
    R<List<UserBriefDTO>> getUsersByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/addresses/{id}")
    R<AddressDTO> getAddressById(@PathVariable("id") Long id);
}
