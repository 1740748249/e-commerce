package com.ecommerce.product.controller;

import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.FlashSessionDTO;
import com.ecommerce.product.domain.vo.FlashSessionVO;
import com.ecommerce.product.service.IEFlashSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/flash-sessions")
@RequiredArgsConstructor
public class AdminFlashSessionController {

    private final IEFlashSessionService flashSessionService;

    @PostMapping
    public R<FlashSessionVO> create(@Valid @RequestBody FlashSessionDTO dto) {
        return flashSessionService.createSession(dto);
    }

    @GetMapping
    public R<PageDTO<FlashSessionVO>> list(PageQuery query) {
        return flashSessionService.listSessions(query);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FlashSessionDTO dto) {
        return flashSessionService.updateSession(id, dto);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        return flashSessionService.deleteSession(id);
    }
}
