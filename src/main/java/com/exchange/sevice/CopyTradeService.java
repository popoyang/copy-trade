package com.exchange.sevice;


import com.exchange.enums.AccountType;

public interface CopyTradeService {

    void syncAndReplicatePositions(String portfolioId);
}

