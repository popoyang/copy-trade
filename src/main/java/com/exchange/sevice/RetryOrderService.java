package com.exchange.sevice;

import com.exchange.enums.AccountType;

import java.math.BigDecimal;

public interface RetryOrderService {

    void placeMarketOrderWithRetry(AccountType accountType, String symbol, String side, String positionSide, BigDecimal quantity);


   void submitDelayedClose(AccountType accountType,String symbol, String positionSide, BigDecimal quantity, String key, ClosePositionCallback callback);
}