package com.ecommerce.product.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(description = "审核操作")
public class ApprovalDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "审核结果不能为空")
    @ApiModelProperty(value = "true=通过, false=拒绝", required = true)
    private Boolean approved;

    @ApiModelProperty(value = "拒绝原因（拒绝时必填）")
    private String rejectReason;
}
