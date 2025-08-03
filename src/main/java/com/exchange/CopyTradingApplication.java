package com.exchange;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitScan;
import com.exchange.config.BaseurlConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({BaseurlConfig.class})
@SpringBootApplication(scanBasePackages = "com.exchange")
@RetrofitScan("com.exchange")
public class CopyTradingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CopyTradingApplication.class, args);
	}

}
