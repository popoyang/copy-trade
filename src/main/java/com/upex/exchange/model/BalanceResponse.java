package com.upex.exchange.model;

import lombok.Data;

@Data
public class BalanceResponse {
    private String accountAlias;
    private String asset;
    private String balance;
    private String crossWalletBalance;
    private String crossUnPnl;
    private String availableBalance;
    private String maxWithdrawAmount;
    private Boolean marginAvailable;
    private Long updateTime;
}

