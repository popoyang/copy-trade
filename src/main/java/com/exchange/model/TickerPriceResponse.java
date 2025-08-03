package com.exchange.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TickerPriceResponse {
    private String symbol;
    private BigDecimal price;
}

