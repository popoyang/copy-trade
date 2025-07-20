package com.upex.exchange.model;

import lombok.Data;

@Data
public class OrderResponse {
    private Long orderId;
    private String symbol;
    private String status;
    private String clientOrderId;
    private String price;
    private String origQty;
    private String executedQty;
    private String type;
    private String side;
}
