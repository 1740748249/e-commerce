package com.ecommerce.order.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.order.domain.dto.CartAddDTO;
import com.ecommerce.order.domain.dto.CartUpdateDTO;
import com.ecommerce.order.domain.vo.CartVO;
import com.ecommerce.order.service.IECartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/cart")
@Slf4j
@RequiredArgsConstructor
public class ECartController {

    private final IECartService cartService;

    @GetMapping
    public R<List<CartVO>> list() {
        return cartService.getCartList();
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        return cartService.add(dto);
    }

    @PutMapping("/{cartItemId}")
    public R<Void> update(@PathVariable Long cartItemId, @Valid @RequestBody CartUpdateDTO dto) {
        return cartService.update(cartItemId, dto.getQuantity());
    }

    @DeleteMapping("/{cartItemId}")
    public R<Void> remove(@PathVariable Long cartItemId) {
        return cartService.remove(cartItemId);
    }

    @DeleteMapping
    public R<Void> clear() {
        return cartService.clear();
    }
}
