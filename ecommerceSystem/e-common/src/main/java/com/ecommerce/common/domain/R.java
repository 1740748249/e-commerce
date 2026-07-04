package com.ecommerce.common.domain;

import com.ecommerce.common.constants.Constant;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.slf4j.MDC;

import static com.ecommerce.common.constants.ErrorInfo.Code.FAILED;
import static com.ecommerce.common.constants.ErrorInfo.Code.SUCCESS;
import static com.ecommerce.common.constants.ErrorInfo.Msg.OK;

@Data
@ApiModel(description = "通用响应结果")
public class R<T> {
    @ApiModelProperty(value = "业务状态码，200-成功，其它-失败")
    private int code;
    @ApiModelProperty(value = "响应消息", example = "success")
    private String message;
    @ApiModelProperty(value = "响应数据")
    private T data;
    @ApiModelProperty(value = "请求id", example = "1af123c11412e")
    private String requestId;

    public static R<Void> ok() {
        return new R<Void>(SUCCESS, OK, null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS, OK, data);
    }

    public static <T> R<T> error(String message) {
        return new R<>(FAILED, message, null);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = MDC.get(Constant.REQUEST_ID_HEADER);
    }

    public boolean success(){
        return code == SUCCESS;
    }

    public R<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
