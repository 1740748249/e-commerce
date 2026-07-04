package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.domain.po.EPaymentRecord;
import com.ecommerce.payment.mapper.EPaymentRecordMapper;
import com.ecommerce.payment.service.IEPaymentRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 支付流水记录表 服务实现类
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-29
 */
@Service
public class EPaymentRecordServiceImpl extends ServiceImpl<EPaymentRecordMapper, EPaymentRecord> implements IEPaymentRecordService {

}
