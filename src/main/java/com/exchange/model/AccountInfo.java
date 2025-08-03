package com.exchange.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountInfo {
    private BigDecimal totalInitialMargin;
    private BigDecimal totalMaintMargin;
    private BigDecimal totalWalletBalance;
    private BigDecimal totalUnrealizedProfit;
    private BigDecimal totalMarginBalance;
    private BigDecimal totalPositionInitialMargin;
    private BigDecimal totalOpenOrderInitialMargin;
    private BigDecimal totalCrossWalletBalance;
    private BigDecimal totalCrossUnPnl;
    private BigDecimal availableBalance;
    private BigDecimal maxWithdrawAmount;

    private List<AssetBalance> assets;
    private List<Position> positions;
}
