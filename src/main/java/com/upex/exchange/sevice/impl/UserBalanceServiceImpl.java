package com.upex.exchange.sevice.impl;

import com.binance.connector.futures.client.impl.UMFuturesClientImpl;
import com.upex.exchange.sevice.UserBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;


@Slf4j
@Service
public class UserBalanceServiceImpl implements UserBalanceService {

    public void getBalance(){
        String path = "fapi/v2/balance";
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

        UMFuturesClientImpl client = new UMFuturesClientImpl("API_KEY", "SECRET_KEY", "");
        String result = client.account().futuresAccountBalance(parameters);
    }
}
