package com.ecommerce.user.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.api.client.OrderClient;
import com.ecommerce.api.client.ProductClient;
import com.ecommerce.api.dto.OrderStatisticsDTO;
import com.ecommerce.api.dto.UserBriefDTO;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.user.domain.query.UserPageQuery;
import com.ecommerce.user.config.JwtProperties;
import com.ecommerce.user.domain.dto.LoginDTO;
import com.ecommerce.user.domain.po.EUser;
import com.ecommerce.user.domain.vo.AdminStatisticsVO;
import com.ecommerce.user.domain.vo.LoginVO;
import com.ecommerce.user.domain.vo.UserVO;
import com.ecommerce.user.enums.UserRole;
import com.ecommerce.user.enums.UserStatus;
import com.ecommerce.user.mapper.EUserMapper;
import com.ecommerce.user.service.IAdminService;
import com.ecommerce.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<EUserMapper, EUser> implements IAdminService {
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public R<LoginVO> login(LoginDTO dto) {
        EUser eUser = lambdaQuery()
                .eq(EUser::getUsername, dto.getUsername())
                .eq(EUser::getStatus, UserStatus.ACTIVE)
                .one();
        if(eUser==null){
            return R.error("用户不存在或账号已被禁用");
        }
        if(!new BCryptPasswordEncoder().matches(dto.getPassword(), eUser.getPassword())){
            return R.error("密码错误");
        }
        if(eUser.getRole() != UserRole.ADMIN){
            return R.error("无管理员权限");
        }
        //密码一致，调用jwt生成token
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(jwtTool.createToken(eUser.getId(), eUser.getRole().getValue(), Duration.parse(jwtProperties.getTokenTTL())));
        UserVO userVO = BeanUtils.copyBean(eUser, UserVO.class);
        LocalDateTime now = LocalDateTime.now();
        userVO.setLastLoginTime(now);
        eUser.setLastLoginTime(now);
        updateById(eUser);
        log.info("超级管理员登录成功");
        return R.ok(loginVO);
    }

    @Override
    public R<PageDTO<UserVO>> getUserList(UserPageQuery query) {
        // TODO: 后期通过ElasticSearch实现
        Page<EUser> page = lambdaQuery()
                .eq(query.getRole() != null, EUser::getRole, query.getRole())
                .eq(query.getStatus() != null, EUser::getStatus, query.getStatus())
                .between(query.getStartTime() != null && query.getEndTime() != null, EUser::getCreateTime, query.getStartTime(), query.getEndTime())
                .and(query.getKeyword() != null, w -> w
                        .like(EUser::getUsername, query.getKeyword())
                        .or()
                        .like(EUser::getName, query.getKeyword())
                        .or()
                        .like(EUser::getPhone, query.getKeyword())
                )
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
       return R.ok(PageDTO.of(page, UserVO.class));

    }

    @Override
    public R<Void> updateUserStatus(Long id, UserStatus status) {
        boolean success = lambdaUpdate()
                .eq(EUser::getId, id)
                .set(EUser::getStatus, status)
                .update();
        if (!success) {
            return R.error("更新用户状态失败");
        }
        log.info("更新用户状态成功");
        return R.ok();
    }

    /**
     * 更新用户角色
     * @param id 商家id
     * @param role
     * @return
     */
    @Override
    public R<Void> updateUserRole(Long id, UserRole role) {
        boolean success = lambdaUpdate()
                .eq(EUser::getId, id)
                .set(EUser::getRole, role)
                .update();
        if (!success){
            throw new BizIllegalException("更新用户角色失败");
        }
        log.info("更新用户角色成功");
        return R.ok();
    }

    /**
     * 批量获取用户信息
     * @param ids
     * @return
     */
    @Override
    public R<List<UserBriefDTO>> getUsersByIds(List<Long> ids) {
        List<EUser> userInfos = lambdaQuery()
                .in(EUser::getId, ids)
                .list();
        if(CollUtils.isEmpty(userInfos)){
            return R.ok(CollUtils.emptyList());
        }
        List<UserBriefDTO> dtoList = BeanUtils.copyList(userInfos, UserBriefDTO.class);
        return R.ok(dtoList);
    }

    @Override
    public R<AdminStatisticsVO> getStatistics() {
        // 1. 查 Redis 缓存，命中直接返回
        String cacheKey = "admin:statistics";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return R.ok(JSONUtil.toBean(cached, AdminStatisticsVO.class));
        }

        // 2. 本地统计
        Long totalUsers = lambdaQuery().count();
        Long totalMerchants = lambdaQuery().eq(EUser::getRole, UserRole.VENDOR).count();

        // 3. 跨服务统计（Feign 容错：失败返回 0）
        Long pendingMerchants = 0L;
        R<Long> shopR = productClient.getPendingShopCount();
        if (shopR.success() && shopR.getData() != null) {
            pendingMerchants = shopR.getData();
        }

        Long totalOrders = 0L;
        Long totalSales = 0L;
        R<OrderStatisticsDTO> orderR = orderClient.getOrderStatistics();
        if (orderR.success() && orderR.getData() != null) {
            OrderStatisticsDTO os = orderR.getData();
            totalOrders = os.getTotalOrders() != null ? os.getTotalOrders() : 0L;
            totalSales = os.getTotalSales() != null ? os.getTotalSales() : 0L;
        }

        // 4. 组装
        AdminStatisticsVO vo = new AdminStatisticsVO();
        vo.setTotalUsers(totalUsers);
        vo.setTotalMerchants(totalMerchants);
        vo.setPendingMerchants(pendingMerchants);
        vo.setTotalOrders(totalOrders);
        vo.setTotalSales(totalSales);

        // 5. 写入缓存（3 分钟 TTL，自然过期后懒加载刷新）
        redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(vo), Duration.ofMinutes(3));

        return R.ok(vo);
    }
}
