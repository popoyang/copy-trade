package com.exchange.model;

import lombok.Data;

@Data
public class PortfolioParam {
    private int pageNumber;
    private int pageSize;
    private String timeRange;
    private String dataType;
    private int daysTrading;
    private boolean favoriteOnly;
    private boolean hideFull;
    private String nickname;
    private String order;
    private int userAsset;
    private String portfolioType;
    private boolean useAiRecommended;
    private int roi;
    private boolean apiKeyOnly;
    private Integer lockPeriod;
}
