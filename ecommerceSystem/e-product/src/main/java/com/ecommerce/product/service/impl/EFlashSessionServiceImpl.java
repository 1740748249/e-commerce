package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.common.cache.CacheService;
import com.ecommerce.common.domain.R;
import com.ecommerce.common.domain.dto.PageDTO;
import com.ecommerce.common.domain.query.PageQuery;
import com.ecommerce.common.exception.BizIllegalException;
import com.ecommerce.common.utils.BeanUtils;
import com.ecommerce.common.utils.CollUtils;
import com.ecommerce.product.domain.dto.FlashSessionDTO;
import com.ecommerce.product.domain.po.EFlashSale;
import com.ecommerce.product.domain.po.EFlashSession;
import com.ecommerce.product.domain.vo.FlashSessionVO;
import com.ecommerce.product.enums.ApprovalStatus;
import com.ecommerce.product.enums.FlashSessionStatus;
import com.ecommerce.product.mapper.EFlashSaleMapper;
import com.ecommerce.product.mapper.EFlashSessionMapper;
import com.ecommerce.product.service.IEFlashSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.ecommerce.product.constants.CacheConstants.SESSION_ALL_KEY;
import static com.ecommerce.product.constants.CacheConstants.SESSION_TTL;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EFlashSessionServiceImpl extends ServiceImpl<EFlashSessionMapper, EFlashSession> implements IEFlashSessionService {

    private final EFlashSaleMapper flashSaleMapper;
    private final CacheService cacheService;

    @Override
    public R<FlashSessionVO> createSession(FlashSessionDTO dto) {
        // 校验时间合法性
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new BizIllegalException("开始时间必须早于结束时间");
        }
        EFlashSession session = BeanUtils.copyBean(dto, EFlashSession.class);
        session.setStatus(FlashSessionStatus.UPCOMING);
        save(session);
        cacheService.delete(SESSION_ALL_KEY);
        FlashSessionVO vo = BeanUtils.copyBean(session, FlashSessionVO.class);
        vo.setStatusText(FlashSessionStatus.UPCOMING.getDesc());
        return R.ok(vo);
    }

    @Override
    public R<PageDTO<FlashSessionVO>> listSessions(PageQuery query) {
        Page<EFlashSession> result = lambdaQuery()
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<EFlashSession> records = result.getRecords();
        if (CollUtils.isEmpty(records)) {
            return R.ok(PageDTO.empty(result));
        }
        // 批量查询各场次的商品报名数
        Set<Long> sessionIds = records.stream()
                .map(EFlashSession::getId).collect(Collectors.toSet());
        Map<Long, Long> countMap = new LambdaQueryChainWrapper<>(flashSaleMapper)
                .in(EFlashSale::getSessionId, sessionIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(EFlashSale::getSessionId, Collectors.counting()));
        // 组装VO
        LocalDateTime now = LocalDateTime.now();
        List<FlashSessionVO> voList = records.stream().map(session -> {
            FlashSessionVO vo = BeanUtils.copyBean(session, FlashSessionVO.class);
            vo.setStatus(calcStatus(session, now));
            vo.setStatusText(FlashSessionStatus.of(vo.getStatus()).getDesc());
            vo.setItemCount(countMap.getOrDefault(session.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(PageDTO.of(result, voList));
    }

    @Override
    public R<Void> updateSession(Long id, FlashSessionDTO dto) {
        EFlashSession session = getById(id);
        if (session == null) {
            throw new BizIllegalException("场次不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (calcStatus(session, now) != FlashSessionStatus.UPCOMING.getValue()) {
            throw new BizIllegalException("仅未开始的场次可编辑");
        }
        if (dto.getName() != null) {
            session.setName(dto.getName());
        }
        if (dto.getStartTime() != null) {
            session.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            session.setEndTime(dto.getEndTime());
        }
        if (!session.getStartTime().isBefore(session.getEndTime())) {
            throw new BizIllegalException("开始时间必须早于结束时间");
        }
        updateById(session);
        cacheService.delete(SESSION_ALL_KEY);
        return R.ok();
    }

    @Override
    public R<Void> deleteSession(Long id) {
        EFlashSession session = getById(id);
        if (session == null) {
            throw new BizIllegalException("场次不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (calcStatus(session, now) != FlashSessionStatus.UPCOMING.getValue()) {
            throw new BizIllegalException("仅未开始的场次可删除");
        }
        // 校验该场次下是否有已通过的报名
        Long count = new LambdaQueryChainWrapper<>(flashSaleMapper)
                .eq(EFlashSale::getSessionId, id)
                .eq(EFlashSale::getApprovalStatus, ApprovalStatus.APPROVED)
                .count();
        if (count > 0) {
            throw new BizIllegalException("该场次下存在已通过的秒杀报名，无法删除");
        }
        removeById(id);
        cacheService.delete(SESSION_ALL_KEY);
        return R.ok();
    }

    @Override
    public R<List<FlashSessionVO>> listAvailable() {
        Map<String, EFlashSession> sessionMap = cacheService.hGetAllOrLoad(
                SESSION_ALL_KEY, SESSION_TTL, EFlashSession.class,
                () -> {
                    List<EFlashSession> all = list();
                    return all.stream().collect(Collectors.toMap(
                            s -> s.getId().toString(), s -> s));
                });
        LocalDateTime now = LocalDateTime.now();
        List<FlashSessionVO> voList = sessionMap.values().stream()
                .filter(s -> calcStatus(s, now) == FlashSessionStatus.UPCOMING.getValue())
                .map(s -> {
                    FlashSessionVO vo = BeanUtils.copyBean(s, FlashSessionVO.class);
                    vo.setStatus(FlashSessionStatus.UPCOMING.getValue());
                    vo.setStatusText(FlashSessionStatus.UPCOMING.getDesc());
                    return vo;
                })
                .collect(Collectors.toList());
        return R.ok(voList);
    }

    private int calcStatus(EFlashSession session, LocalDateTime now) {
        if (now.isBefore(session.getStartTime())) {
            return FlashSessionStatus.UPCOMING.getValue();
        } else if (now.isAfter(session.getEndTime())) {
            return FlashSessionStatus.ENDED.getValue();
        } else {
            return FlashSessionStatus.IN_PROGRESS.getValue();
        }
    }
}
