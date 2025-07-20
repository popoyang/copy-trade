package com.upex.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class PortfolioQueryRequest {
    private String portfolioId;
    private long startTime;
    private long endTime;
    private int pageSize;
}

