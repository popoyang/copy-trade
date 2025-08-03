package com.exchange.config;

import com.exchange.sevice.ExchangeInfoService;
import com.exchange.util.StepSizeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AppConfig {
    @Autowired
    private ExchangeInfoService exchangeInfoService;

    @PostConstruct
    public void init() {
        StepSizeUtil.setExchangeInfoService(exchangeInfoService);
    }
}

