package com.exchange.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetBalance {
    private String asset;
    private String walletBalance;
    private String unrealizedProfit;
    private String marginBalance;
    private String maintMargin;
    private String initialMargin;
    private String positionInitialMargin;
    private String openOrderInitialMargin;
    private String maxWithdrawAmount;
    private String crossWalletBalance;
    private String crossUnPnl;
    private String availableBalance;
}
