package com.upex.exchange.model;

import lombok.Data;

import java.util.List;

@Data
public class ExchangeInfoResponse {
    private List<Object> exchangeFilters;
    private List<RateLimit> rateLimits;
    private long serverTime;
    private List<Asset> assets;
    private List<SymbolInfo> symbols;
    private String timezone;

}
