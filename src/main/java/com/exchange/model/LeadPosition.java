package com.exchange.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class LeadPosition implements Serializable {
    private static final long serialVersionUID = -2256478421690187884L;
    private String id;
    /**
     * 合约交易对，如 ONDOUSDT
     */
    private String symbol;
    private String collateral;
    /**
     * 当前仓位数量，为 "0" 表示当前无仓
     */
    private String positionAmount;
    /**
     * 开仓均价
     */
    private String entryPrice;
    /**
     * 未实现盈亏
     */
    private String unrealizedProfit;
    /**
     * 累计已实现盈亏
     */
    private String cumRealized;
    private String askNotional;
    private String bidNotional;
    private String notionalValue;
    private String markPrice;
    /**
     * 杠杆倍数
     */
    private int leverage;
    private boolean isolated;
    private String isolatedWallet;
    private int adl;
    /**
     * 持仓方向：BOTH（双向）、LONG、SHORT
     */
    private String positionSide;
    private String breakEvenPrice;

    public boolean hasPosition() {
        if (positionAmount == null || positionAmount.trim().isEmpty()) {
            return false;
        }
        try {
            BigDecimal amt = new BigDecimal(positionAmount.trim());
            return amt.compareTo(BigDecimal.ZERO) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
