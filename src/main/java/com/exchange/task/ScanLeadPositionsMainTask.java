package com.exchange.task;

import com.exchange.enums.AccountType;
import com.exchange.sevice.CopyTradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "lead.positions.task.enabled", havingValue = "true", matchIfMissing = true)
public class ScanLeadPositionsMainTask {

    @Autowired
    private CopyTradeService copyTradeService;

    @Value("${binance.main.portfolioId}")
    private String portfolioId;

    @Scheduled(fixedRateString = "${lead.positions.main.task.fixedRate:130}")
    public void scanAndReplicatePositions() {
        try {
            copyTradeService.syncAndReplicatePositions(portfolioId, AccountType.MAIN);
        } catch (Exception e) {
            log.error("ScanLeadPositionsTask 执行异常", e);
        }
    }
}

