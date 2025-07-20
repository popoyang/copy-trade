package com.upex.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CopyTradingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CopyTradingApplication.class, args);
	}

}
