package com.ecommerce.user.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.query.UserPageQuery;
import com.ecommerce.user.domain.vo.AdminStatisticsVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.service.IAdminService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin")
@Slf4j
@Api(tags = "管理员相关接口")
@RequiredArgsConstructor
public class AdminController {
    private final IAdminService adminService;

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return adminService.login(dto);
    }

    @GetMapping("/users")
    public R<PageDTO<UserVO>> getUserList(UserPageQuery query) {
        return adminService.getUserList(query);
    }

    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id,
                                    @RequestParam UserStatus status) {
        return adminService.updateUserStatus(id, status);
    }

    @PutMapping("/users/{id}/role")
    public R<Void> updateUserRole(@PathVariable Long id,
                                   @RequestParam UserRole role) {
        return adminService.updateUserRole(id, role);
    }

    @GetMapping("/users/batch")
    public R<List<UserBriefDTO>> getUsersByIds(@RequestParam List<Long> ids) {
        return adminService.getUsersByIds(ids);
    }

    @GetMapping("/statistics")
    public R<AdminStatisticsVO> getStatistics() {
        return adminService.getStatistics();
    }
}
