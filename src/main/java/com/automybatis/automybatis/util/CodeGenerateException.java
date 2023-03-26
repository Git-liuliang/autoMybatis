package com.automybatis.automybatis.util;


public class CodeGenerateException extends RuntimeException {

    private static final long serialVersionUID = 42L;

    public CodeGenerateException() {
        super();
    }

    public CodeGenerateException(String msg) {
        super(msg);
    }

    public CodeGenerateException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CodeGenerateException(Throwable cause) {
        super(cause);
    }

    public CodeGenerateException(String message, Throwable cause,
                                 boolean enableSuppression,
                                 boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
