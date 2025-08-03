package com.exchange.model;

import lombok.Data;

@Data
public class PositionRisk {
    private String symbol;
    private String positionAmt;    // 仓位数量（正数为多仓，负数为空仓）
    private String entryPrice;     // 持仓均价
    private String markPrice;      // 标记价格
    private String unrealizedProfit;
    private String leverage;
    private String marginType;
    private Boolean isolated;
}
