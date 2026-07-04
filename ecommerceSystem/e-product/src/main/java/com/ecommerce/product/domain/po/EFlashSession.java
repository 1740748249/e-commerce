package com.ecommerce.product.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.product.enums.FlashSessionStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("e_flash_session")
@ApiModel(value = "EFlashSession对象", description = "秒杀场次表")
public class EFlashSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "场次名称")
    private String name;

    @ApiModelProperty(value = "场次开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "场次结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "状态: 0=未开始, 1=进行中, 2=已结束")
    private FlashSessionStatus status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
