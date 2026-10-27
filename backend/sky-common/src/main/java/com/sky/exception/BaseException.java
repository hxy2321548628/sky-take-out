package com.sky.exception;

/**
 * 业务异常
 */
public class BaseException extends RuntimeException {

    // 这里是重载
    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }

}
