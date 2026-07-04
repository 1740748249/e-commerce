package com.ecommerce.common.exception;

import lombok.Getter;

@Getter
public class CommonException extends RuntimeException{
    private int code;
    private int status;

    public CommonException(String message) {
        super(message);
        this.code = 500;
        this.status = 500;
    }

    public CommonException(String message, int code) {
        super(message);
        this.code = code;
        this.status = 500;
    }

    public CommonException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
        this.status = 500;
    }

    public CommonException(Throwable cause, int code) {
        super(cause);
        this.code = code;
        this.status = 500;
    }

    public CommonException(String message, int code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public CommonException(String message, Throwable cause, int code, int status) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    public CommonException(Throwable cause, int code, int status) {
        super(cause);
        this.code = code;
        this.status = status;
    }
}
