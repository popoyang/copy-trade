package com.exchange.model;

import lombok.Data;

@Data
public class SymbolFilter {
    private String filterType;

    private String maxPrice;
    private String minPrice;
    private String tickSize;

    private String maxQty;
    private String minQty;
    private String stepSize;

    private Integer limit;

    private String notional;

    private String multiplierUp;
    private String multiplierDown;
    private String multiplierDecimal;
}
