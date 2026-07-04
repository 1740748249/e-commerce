package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.product.domain.dto.ApprovalDTO;
import com.ecommerce.product.domain.query.FlashSaleApplicationQuery;
import com.ecommerce.product.domain.vo.FlashSaleApplicationVO;
import com.ecommerce.product.service.IEFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/flash-sales")
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final IEFlashSaleService flashSaleService;

    @GetMapping("/applications")
    public R<PageDTO<FlashSaleApplicationVO>> applications(FlashSaleApplicationQuery query) {
        return flashSaleService.listApplications(query);
    }

    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @Valid @RequestBody ApprovalDTO dto) {
        return flashSaleService.approve(id, dto);
    }
}
