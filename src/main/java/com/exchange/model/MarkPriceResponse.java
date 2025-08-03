package com.exchange.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarkPriceResponse {
    private String symbol;
    private BigDecimal markPrice;
    private Long time;
}
