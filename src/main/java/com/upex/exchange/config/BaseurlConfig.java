package com.upex.exchange.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "baseurl")
public class BaseurlConfig {

    @Value("webUrl")
    private String webUrl;

    @Value("apiUrl")
    private String apiUrl;
}
