package com.upex.exchange.model;

import lombok.Data;

@Data
public class Order {
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private String side;
    private String type;
    private String positionSide;
    private double executedQty;
    private double avgPrice;
    private double totalPnl;
    private long orderUpdateTime;
    private long orderTime;
}
