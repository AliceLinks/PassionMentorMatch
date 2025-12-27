package com.example.mentor.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;
    private final String errorCode;

    public BizException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public static BizException conflict(String message) {
        return new BizException(409, "409", message);
    }

    public static BizException badRequest(String message) {
        return new BizException(400, "400", message);
    }
}
