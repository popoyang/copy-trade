package com.upex.exchange.task;

import com.alibaba.fastjson.JSON;
import com.upex.exchange.model.Order;
import com.upex.exchange.model.PortfolioQueryRequest;
import com.upex.exchange.sevice.LeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
public class OrderHistoryTask {

    @Autowired
    private LeadService leadService;

    private long lastOrderTime = System.currentTimeMillis();

    // 后续考虑 xxl-job 参数化配置
    @Scheduled(fixedRate = 6000)
    public void executeOrderHistoryTask() {
        try {
            // TODO 后续试情况调整频率
            long startTime = System.currentTimeMillis() - 86400000;
            long endTime = LocalDate.now()
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            String portfolioId = "4555210807627654913";

            PortfolioQueryRequest request = new PortfolioQueryRequest(
                    portfolioId,
                    startTime,
                    endTime,
                    10
            );

            List<Order> orders = leadService.getOrderHistoryList(request);
            if (!orders.isEmpty()) {
                log.info("Fetched orders: {}", JSON.toJSONString(orders));
                long maxOrderTime = orders.get(orders.size() - 1).getOrderTime();
                if (maxOrderTime <= lastOrderTime) {
                    log.info("No new orders detected. Last known order time: {}", lastOrderTime);
                    return; // 提前返回，不再处理
                }

                for (Order order : orders) {
                    long orderTime = order.getOrderTime();
                    if (orderTime > lastOrderTime) {
                        // 查询带单员保证金
                        BigDecimal leadMarginBalance = leadService.getLeadMarginBalance(portfolioId);
                        log.info("Lead margin balance: {}", leadMarginBalance);
                        //查询本身用户保证金计算比例
                        log.info("New order detected: {}", JSON.toJSONString(order));
                        // 模拟下单操作（你自己的逻辑）
                        // 更新已处理时间戳

                        lastOrderTime = orderTime;
                    }
                }
            } else {
                log.info("No orders found in this period.");
            }


        } catch (Exception e) {
            log.error("Failed to fetch order history", e);
        }
    }
}
