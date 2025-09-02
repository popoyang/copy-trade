package com.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.exchange.enums.AccountType;
import com.exchange.model.LeadPosition;
import com.exchange.model.OrderResponse;
import com.exchange.model.OrderSide;
import com.exchange.sevice.ClosePositionCallback;
import com.exchange.sevice.LeadService;
import com.exchange.sevice.OrderService;
import com.exchange.sevice.RetryOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RetryOrderServiceImpl implements RetryOrderService {
    @Autowired
    private OrderService orderService;

    @Autowired
    private LeadService leadService;

    @Value("${binance.portfolioId}")
    private String portfolioId;

    @Value("${copy.trade.order.retry.times:100}")
    private int maxRetryTimes;

    @Value("${copy.trade.order.retry.delay:1000}")
    private long retryDelay;

    @Value("${copy.trade.order.delay.millis:2000}")
    private long delayMillis;




    @Override
    public void placeMarketOrderWithRetry(AccountType accountType, String symbol, String side, String positionSide, BigDecimal quantity) {
        for (int i = 1; i <= maxRetryTimes; i++) {
            try {
                OrderResponse response = orderService.placeMarketOrder(accountType,symbol, side, positionSide, quantity);
                if (response != null && response.getOrderId() != null) {
                    log.info("下单成功：symbol={} side={} posSide={} qty={} 订单ID={}",
                            symbol, side, positionSide, quantity, response.getOrderId());
                    return;
                } else {
                    log.warn("第{}次下单返回空或失败，symbol={} qty={}", i, symbol, quantity);
                }
            } catch (Exception e) {
                log.error("第{}次下单异常，symbol={}, qty={}, error={}", i, symbol, quantity, e.getMessage());
            }

            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ignored) {}
        }
        log.error("下单重试失败，symbol={} side={} posSide={} qty={}", symbol, side, positionSide, quantity);
    }

    @Override
    public void submitDelayedClose(AccountType accountType,String symbol, String positionSide, BigDecimal quantity, String key, ClosePositionCallback callback) {
        // 新线程异步处理延迟平仓，防止阻塞调用线程
        new Thread(() -> {
            boolean closed = false;
            try {
                Thread.sleep(delayMillis);

                // 重新查询最新持仓，判断是否仍需平仓
                LeadPosition latestPos = leadService.getPositionBySymbolAndSide(portfolioId,symbol, positionSide);
                BigDecimal latestQty = latestPos != null ? new BigDecimal(latestPos.getPositionAmount()) : BigDecimal.ZERO;
                BigDecimal myOrderQty = orderService.getMyPositionQuantity(accountType,symbol, positionSide);
                // 若最新仓位仍为0或不存在，确认平仓数量执行下单
                if (latestQty.compareTo(BigDecimal.ZERO) == 0) {
                    OrderSide closeSide = getCloseSide(positionSide, quantity);
                    log.info("[延迟平仓确认] symbol={} positionSide={} 下单数量={} myOrderQty={} closeSide={}", symbol, positionSide, quantity, myOrderQty, JSON.toJSONString(closeSide));
                    // 下单，带重试
                    placeMarketOrderWithRetry(accountType,
                            symbol,
                            closeSide.getOrderSide(),
                            closeSide.getPositionSide(),
                            myOrderQty);
                    closed = true;
                } else {
                    log.info("[延迟平仓取消] symbol={} positionSide={} 最新持仓数量={}，无需平仓", symbol, positionSide, latestQty);
                }
            } catch (InterruptedException e) {
                log.error("延迟平仓线程中断", e);
            } catch (Exception e) {
                log.error("延迟平仓执行异常", e);
            } finally {
                // 通知调用方移除状态
                if (callback != null) {
                    callback.onCloseComplete(key, closed);
                }
            }
        }).start();
    }

    private OrderSide getCloseSide(String positionSide,BigDecimal positionAmount) {
        if ("BOTH".equalsIgnoreCase(positionSide)) {
            // 当前是平仓
            if (positionAmount.compareTo(BigDecimal.ZERO) > 0) {
                return new OrderSide("SELL", "LONG");
            } else if (positionAmount.compareTo(BigDecimal.ZERO) < 0) {
                // 平空仓（BUY）
                return new OrderSide("BUY", "SHORT");
            }
        } else {
            // 如果是 LONG 或 SHORT，正常的开仓或平仓操作
            if ("LONG".equalsIgnoreCase(positionSide)) {
                return new OrderSide("SELL", "LONG");
            } else if ("SHORT".equalsIgnoreCase(positionSide)) {
                return new OrderSide("BUY", "SHORT");
            }
        }
        throw new IllegalArgumentException("Invalid position side: " + positionSide);
    }
}
