package com.exchange.sevice;

import java.math.BigDecimal;

public interface RetryOrderService {

    void placeMarketOrderWithRetry(String symbol, String side, String positionSide, BigDecimal quantity);


   void submitDelayedClose(String symbol, String positionSide, BigDecimal quantity, String key, ClosePositionCallback callback);
}