package com.upex.exchange.model;

import lombok.Data;

import java.util.List;

@Data
public class Portfolio {
    private String leadPortfolioId;
    private String nickname;
    private String avatarUrl;
    private int currentCopyCount;
    private int maxCopyCount;
    private double roi;
    private double pnl;
    private double aum;
    private double mdd;
    private double winRate;
    private String apiKeyTag;
    private double sharpRatio;
    private List<ChartItem> chartItems;
    private String badgeName;
    private long badgeModifyTime;
    private int badgeCopierCount;
    private String portfolioType;
}

