package com.upex.exchange;

import com.upex.exchange.config.BaseurlConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({BaseurlConfig.class})
public class CopyTradingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CopyTradingApplication.class, args);
	}

}
