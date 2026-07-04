package com.ecommerce.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.product.domain.dto.FlashSessionDTO;
import com.ecommerce.product.domain.po.EFlashSession;
import com.ecommerce.product.domain.vo.FlashSessionVO;

import java.util.List;

public interface IEFlashSessionService extends IService<EFlashSession> {

    R<FlashSessionVO> createSession(FlashSessionDTO dto);

    R<PageDTO<FlashSessionVO>> listSessions(PageQuery query);

    R<Void> updateSession(Long id, FlashSessionDTO dto);

    R<Void> deleteSession(Long id);

    R<List<FlashSessionVO>> listAvailable();
}
