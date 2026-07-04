package com.ecommerce.product.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "秒杀场次创建/编辑")
public class FlashSessionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "场次名称不能为空")
    @ApiModelProperty(value = "场次名称", required = true)
    private String name;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开始时间", required = true)
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "结束时间", required = true)
    private LocalDateTime endTime;
}
