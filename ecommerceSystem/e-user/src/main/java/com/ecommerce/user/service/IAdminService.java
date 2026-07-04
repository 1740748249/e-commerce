package com.ecommerce.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.query.UserPageQuery;
import com.ecommerce.user.domain.po.EUser;
import com.ecommerce.user.domain.vo.AdminStatisticsVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;

import java.util.List;

public interface IAdminService extends IService<EUser> {

    R<LoginVO> login(LoginDTO dto);

    R<PageDTO<UserVO>> getUserList(UserPageQuery query);

    R<Void> updateUserStatus(Long id, UserStatus status);

    R<Void> updateUserRole(Long id, UserRole role);

    R<List<UserBriefDTO>> getUsersByIds(List<Long> ids);

    R<AdminStatisticsVO> getStatistics();
}
