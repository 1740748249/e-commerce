package com.ecommerce.order.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "支付回调")
public class PayCallbackDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "支付流水号不能为空")
    @ApiModelProperty(value = "第三方支付流水号", required = true)
    private String payNo;

    @NotNull(message = "支付时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "支付时间", required = true)
    private LocalDateTime payTime;

    @NotNull(message = "实付金额不能为空")
    @ApiModelProperty(value = "实付金额（分）", required = true)
    private Integer payAmount;
}
