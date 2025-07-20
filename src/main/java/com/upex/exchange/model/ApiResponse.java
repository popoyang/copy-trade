package com.upex.exchange.model;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private String code;
    private String message;
    private String messageDetail;
    private T data;
    private boolean success;

}

