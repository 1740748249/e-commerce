package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.FlashSaleCreateDTO;
import com.ecommerce.product.domain.vo.FlashSaleApplicationVO;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.service.IEFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/merchant/flash-sales")
@RequiredArgsConstructor
public class MerchantFlashSaleController {

    private final IEFlashSaleService flashSaleService;

    @PostMapping
    public R<Void> apply(@Valid @RequestBody FlashSaleCreateDTO dto) {
        return flashSaleService.create(dto);
    }

    @GetMapping("/applications")
    public R<PageDTO<FlashSaleApplicationVO>> myApplications(@RequestParam(required = false) ApprovalStatus approvalStatus,
                                                              PageQuery query) {
        return flashSaleService.myApplications(approvalStatus, query);
    }
}
