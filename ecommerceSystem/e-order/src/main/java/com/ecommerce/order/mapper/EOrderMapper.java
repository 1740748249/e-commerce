package com.ecommerce.order.mapper;

import com.ecommerce.order.domain.po.EOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author 浩哥
 * @since 2026-06-25
 */
public interface EOrderMapper extends BaseMapper<EOrder> {

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM e_order WHERE status IN (1, 2, 3) AND deleted = 0")
    Long sumCompletedSales();
}
