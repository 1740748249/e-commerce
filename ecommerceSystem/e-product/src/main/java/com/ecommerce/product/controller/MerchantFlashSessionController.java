package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.product.domain.vo.FlashSessionVO;
import com.ecommerce.product.service.IEFlashSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/merchant/flash-sessions")
@RequiredArgsConstructor
public class MerchantFlashSessionController {

    private final IEFlashSessionService flashSessionService;

    @GetMapping
    public R<List<FlashSessionVO>> listAvailable() {
        return flashSessionService.listAvailable();
    }
}
