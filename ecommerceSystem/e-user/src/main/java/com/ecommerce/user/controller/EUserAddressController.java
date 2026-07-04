package com.ecommerce.user.controller;


import com.ecommerce.common.domain.R;
import com.ecommerce.user.domain.dto.AddressDTO;
import com.ecommerce.user.domain.vo.AddressVO;
import com.ecommerce.user.service.IEUserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 用户收货地址表 前端控制器
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-17
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Slf4j
public class EUserAddressController {
    private final IEUserAddressService addressService;
    /**
     * 获取当前用户收货地址列表
     * @return
     */
    @GetMapping
    public R<List<AddressVO>> getUserAddresses(){
        return addressService.getUserAddresses();
    }

    /**
     * 获取单个收货地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<AddressVO> getAddressById(@PathVariable Long id){
        return addressService.getAddressById(id);
    }

    /**
     * 新增收货地址
     * @param dto
     * @return
     */
    @PostMapping
    public R<Void> addUserAddress(@Valid @RequestBody AddressDTO dto){
        return addressService.addUserAddress(dto);
    }

    /**
     * 修改收货地址
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/{id}")
    public R<Void> updateUserAddress(@PathVariable Long id, @Valid @RequestBody AddressDTO dto){
        return addressService.updateUserAddress(id, dto);
    }
    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteUserAddress(@PathVariable Long id){
        return addressService.deleteUserAddress(id);
    }
}
