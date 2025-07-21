package com.upex.exchange.task;

import com.upex.exchange.model.Order;
import com.upex.exchange.model.PortfolioQueryRequest;
import com.upex.exchange.sevice.LeadService;
import com.upex.exchange.sevice.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
public class ScanOrderHistoryTask {

    @Autowired
    private LeadService leadService;

    @Autowired
    private OrderService orderService;

    @Value("${binance.portfolioId:4555210807627654913}")
    private String portfolioId;

    private long lastOrderTime = System.currentTimeMillis();

    @Scheduled(fixedRateString = "${order.history.task.fixedRate:300}")
    public void executeOrderHistoryTask() {
        try {

            long startTime = System.currentTimeMillis() - 600000;
            long endTime = LocalDate.now()
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            PortfolioQueryRequest request = new PortfolioQueryRequest(
                    portfolioId,
                    startTime,
                    endTime,
                    10
            );

            List<Order> orders = leadService.getOrderHistoryList(request);

            if (orders == null || orders.isEmpty()) {
                log.info("No orders found between {} and {} for portfolio {}", startTime, endTime, portfolioId);
                return;
            }

            log.info("Fetched {} orders for portfolio {}", orders.size(), portfolioId);

            long maxOrderTime = orders.get(orders.size() - 1).getOrderTime();
            if (maxOrderTime <= lastOrderTime) {
                log.info("No new orders since lastOrderTime={}, skipping processing.", lastOrderTime);
                return;
            }

            lastOrderTime = orderService.processOrders(orders, portfolioId,lastOrderTime);

        } catch (Exception e) {
            log.error("Failed to fetch or process order history", e);
        }
    }

}
