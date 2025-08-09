package com.exchange.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "binance.accounts")
@Data
public class BinanceAccountsConfig {
    private BinanceAccount main;
    private BinanceAccount second;
}

