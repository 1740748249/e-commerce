package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.domain.po.ERefundRecord;
import com.ecommerce.payment.mapper.ERefundRecordMapper;
import com.ecommerce.payment.service.IERefundRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 退款流水记录表 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-29
 */
@Service
public class ERefundRecordServiceImpl extends ServiceImpl<ERefundRecordMapper, ERefundRecord> implements IERefundRecordService {

}
