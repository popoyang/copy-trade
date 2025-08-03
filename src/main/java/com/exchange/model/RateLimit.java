package com.exchange.model;

import lombok.Data;

@Data
public class RateLimit {
    private String interval;
    private int intervalNum;
    private int limit;
    private String rateLimitType;
}
