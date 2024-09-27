package com.solo83.exception;

public class MinioServiceException extends RuntimeException{

    public MinioServiceException(String message) {
        super(message);
    }

    public MinioServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
