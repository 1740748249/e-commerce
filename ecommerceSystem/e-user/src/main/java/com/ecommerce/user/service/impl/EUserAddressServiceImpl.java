package com.ecommerce.user.service.impl;

import cn.hutool.json.JSONUtil;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.JsonUtils;
import com.ecommerce.common.utils.UserContext;
import com.ecommerce.user.domain.dto.AddressDTO;
import com.ecommerce.user.domain.po.EUserAddress;
import com.ecommerce.user.domain.vo.AddressVO;
import com.ecommerce.user.mapper.EUserAddressMapper;
import com.ecommerce.user.service.IEUserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ecommerce.user.constants.RedisConstants.USER_ADDR_KEY;
import static com.ecommerce.user.constants.RedisConstants.USER_ADDR_TTL;

/**
 * <p>
 * 用户收货地址表 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@Service
@RequiredArgsConstructor
public class EUserAddressServiceImpl extends ServiceImpl<EUserAddressMapper, EUserAddress> implements IEUserAddressService {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public R<List<AddressVO>> getUserAddresses() {
        //获取用户id
        Long userId = UserContext.getUserId();
        //先查缓存
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(USER_ADDR_KEY + userId);
        if (!map.isEmpty()) {
            List<AddressVO> list = map.values()
                    .stream()
                    .map(item -> JsonUtils.toBean(item.toString(), AddressVO.class))
                    .collect(Collectors.toList());
            return R.ok(list);
        }
        //缓存未命中，查询数据库
        List<EUserAddress> list = lambdaQuery()
                .eq(EUserAddress::getUserId, userId)
                .orderByDesc(EUserAddress::getUpdateTime).list();
        List<AddressVO> voList = BeanUtils.copyToList(list, AddressVO.class);
        //把查询出的数据添加到缓存中
        Map<String, String> voMap = voList.stream().collect(Collectors.toMap(item -> item.getId().toString(), JSONUtil::toJsonStr));
        stringRedisTemplate.opsForHash().putAll(USER_ADDR_KEY + userId, voMap);
        stringRedisTemplate.expire(USER_ADDR_KEY + userId, USER_ADDR_TTL, TimeUnit.MINUTES);
        return R.ok(voList);
    }

    @Override
    public R<AddressVO> getAddressById(Long id) {
        Long userId = UserContext.getUserId();
        EUserAddress addr = lambdaQuery()
                .eq(EUserAddress::getId, id)
                .eq(EUserAddress::getUserId, userId)
                .one();
        if (addr == null) {
            return R.error("地址不存在");
        }
        return R.ok(BeanUtils.copyBean(addr, AddressVO.class));
    }

    @Override
    public R<Void> addUserAddress(AddressDTO dto) {
        Long userId = UserContext.getUserId();
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            lambdaUpdate()
                    .eq(EUserAddress::getUserId, userId)
                    .eq(EUserAddress::getIsDefault, 1)
                    .set(EUserAddress::getIsDefault, 0)
                    .update();
        }
        EUserAddress eUserAddress = BeanUtils.copyBean(dto, EUserAddress.class);
        eUserAddress.setUserId(userId);
        boolean success = save(eUserAddress);
        if (success) {
            stringRedisTemplate.delete(USER_ADDR_KEY + userId);
            return R.ok();
        }
        return R.error("添加失败");
    }

    @Override
    public R<Void> updateUserAddress(Long id, AddressDTO dto) {
        Long userId = UserContext.getUserId();
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            lambdaUpdate()
                    .eq(EUserAddress::getUserId, userId)
                    .eq(EUserAddress::getIsDefault, 1)
                    .set(EUserAddress::getIsDefault, 0)
                    .update();
        }
        EUserAddress eUserAddress = BeanUtils.copyBean(dto, EUserAddress.class);
        eUserAddress.setId(id);
        boolean success = lambdaUpdate()
                .eq(EUserAddress::getUserId, userId)
                .eq(EUserAddress::getId, id)
                .update(eUserAddress);
        if (success) {
            stringRedisTemplate.delete(USER_ADDR_KEY + userId);
            return R.ok();
        }
        return R.error("更新失败");
    }

    @Override
    public R<Void> deleteUserAddress(Long id) {
        Long userId = UserContext.getUserId();
        boolean success = lambdaUpdate()
                .eq(EUserAddress::getId, id)
                .eq(EUserAddress::getUserId, userId)
                .remove();
        if (success) {
            stringRedisTemplate.delete(USER_ADDR_KEY + userId);
            return R.ok();
        }
        return R.error("删除失败");
    }
}
