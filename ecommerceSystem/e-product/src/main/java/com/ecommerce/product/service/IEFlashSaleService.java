package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.ApprovalDTO;
import com.ecommerce.product.domain.dto.FlashSaleCreateDTO;
import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.domain.query.FlashSaleApplicationQuery;
import com.ecommerce.product.domain.vo.FlashSaleApplicationVO;
import com.ecommerce.product.domain.vo.FlashSaleOrderVO;
import com.ecommerce.product.domain.vo.FlashSaleVO;
import com.ecommerce.product.enums.ApprovalStatus;

import java.util.List;

public interface IEFlashSaleService extends IService<EFlashSale> {

    // ==================== 用户端 ====================

    R<List<FlashSaleVO>> listFlashSales(Long sessionId);

    R<FlashSaleOrderVO> order(Long flashSaleId, Integer quantity, Long addressId);

    R<FlashSaleOrderVO> result(Long flashSaleId);

    // ==================== 管理员端 ====================

    R<PageDTO<FlashSaleApplicationVO>> listApplications(FlashSaleApplicationQuery query);

    R<Void> approve(Long id, ApprovalDTO dto);

    // ==================== 商家端 ====================

    R<Void> create(FlashSaleCreateDTO dto);

    R<PageDTO<FlashSaleApplicationVO>> myApplications(ApprovalStatus approvalStatus, PageQuery query);
}
