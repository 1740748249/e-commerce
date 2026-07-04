package com.ecommerce.common.exception;

public class BizIllegalException extends CommonException{

    public BizIllegalException(String message) {
        super(message, 500, 500);
    }

    public BizIllegalException(String message, Throwable cause) {
        super(message, cause, 500, 500);
    }

    public BizIllegalException(Throwable cause) {
        super(cause, 500, 500);
    }
}
