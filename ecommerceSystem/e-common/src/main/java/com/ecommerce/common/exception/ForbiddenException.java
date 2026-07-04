package com.ecommerce.common.exception;

public class ForbiddenException extends CommonException{

    public ForbiddenException(String message) {
        super(message, 403, 403);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, 403, 403);
    }

    public ForbiddenException(Throwable cause) {
        super(cause, 403, 403);
    }
}
