package com.upex.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.upex.exchange.Api.BinanceApiService;
import com.upex.exchange.common.Constants;
import com.upex.exchange.model.Order;
import com.upex.exchange.model.OrderResponse;
import com.upex.exchange.model.PositionRisk;
import com.upex.exchange.sevice.LeadService;
import com.upex.exchange.sevice.OrderService;
import com.upex.exchange.sevice.UserBalanceService;
import com.upex.exchange.util.HmacSHA256Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private BinanceApiService binanceApiService;

    @Autowired
    private LeadService leadService;

    @Autowired
    private UserBalanceService userBalanceService;

    @Value("${binance.secretKey}")
    private String secretKey;

    @Value("${binance.api.apiKey}")
    private String apiKey;

    @Value("${binance.api.recvWindow:3000}")
    private long recvWindow;

    @Value("${binance.api.multiplier:5}")
    private BigDecimal multiplier;


    @Override
    public OrderResponse placeMarketOrder(
            String symbol,
            String side,
            String positionSide,
            BigDecimal quantity) {
        try {
            // 推荐使用币安服务器时间
            long serverTime = System.currentTimeMillis();
            // 构造参数字符串（注意顺序）
            String queryString = "symbol=" + symbol
                    + "&side=" + side
                    + "&positionSide=" + positionSide
                    + "&type=MARKET"
                    + "&quantity=" + quantity.stripTrailingZeros().toPlainString()
                    + "&timestamp=" + serverTime
                    + "&recvWindow=" + recvWindow;

            String signature = HmacSHA256Utils.sign(queryString, secretKey);

            Call<OrderResponse> call = binanceApiService.placeOrder(
                    symbol,
                    side,
                    positionSide,
                    "MARKET",
                    quantity.toPlainString(),
                    serverTime,
                    signature,
                    recvWindow,
                    apiKey
            );

            Response<OrderResponse> response = call.execute();
            if (response.isSuccessful()) {
                OrderResponse order = response.body();
                log.info("Order placed successfully: {}", JSON.toJSONString(order));
                return order;
            } else {
                log.error("Binance order failed: {}", response.errorBody() != null ? response.errorBody().string() : "unknown error");
            }

        } catch (Exception e) {
            log.error("Exception while placing Binance order", e);
        }
        return null;
    }

    @Override
    public PositionRisk getPositionRisk(String symbol, String positionSide) {
        if (StringUtils.isBlank(symbol) || StringUtils.isBlank(positionSide)) {
            log.warn("Invalid parameters: symbol or positionSide is blank.");
            return null;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;
            String signature = HmacSHA256Utils.sign(queryString, secretKey);

            log.info("Fetching position risk for symbol: {}, positionSide: {}", symbol, positionSide);

            Call<List<PositionRisk>> call = binanceApiService.getPositionRisk(timestamp, recvWindow, signature, apiKey);
            Response<List<PositionRisk>> response = call.execute();

            if (!response.isSuccessful()) {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                log.error("Failed to fetch position risk. Code: {}, Body: {}", response.code(), errorBody);
                return null;
            }

            List<PositionRisk> positionRisks = response.body();
            if (positionRisks == null || positionRisks.isEmpty()) {
                log.warn("No position risk data returned from Binance.");
                return null;
            }

            for (PositionRisk positionRisk : positionRisks) {
                if (symbol.equalsIgnoreCase(positionRisk.getSymbol())) {
                    log.info("Found symbol:{} position risk: {}", symbol, JSON.toJSONString(positionRisk));
                    BigDecimal amt = new BigDecimal(positionRisk.getPositionAmt());
                    if(Constants.BOTH.equalsIgnoreCase(positionSide)){
                        return positionRisk;
                    }
                    if(amt.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                    }
                    if (amt.compareTo(BigDecimal.ZERO) != 0 &&
                            ((amt.compareTo(BigDecimal.ZERO) > 0 && Constants.LONG.equalsIgnoreCase(positionSide)) ||
                                    (amt.compareTo(BigDecimal.ZERO) < 0 && Constants.SHORT.equalsIgnoreCase(positionSide)))) {
                        return positionRisk;
                    }
                }
            }

            log.info("No matching position found for symbol: {}, side: {}", symbol, positionSide);

        } catch (Exception e) {
            log.error("Exception while fetching Binance position risk", e);
        }

        return null;
    }

    @Override
    public long processOrders(List<Order> orders, String portfolioId, long lastOrderTime) {
        BigDecimal leadMarginBalance = leadService.getLeadMarginBalance(portfolioId);
        if (leadMarginBalance == null) {
            log.warn("Lead margin balance is null for portfolio {}", portfolioId);
            return System.currentTimeMillis();
        }

        BigDecimal availableBalance = userBalanceService.getAvailableBalance(Constants.USDT);
        log.info("Available balance for portfolio {},availableBalance:{}", portfolioId, availableBalance);
        if (availableBalance == null || availableBalance.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Available balance is null or zero for user.");
            return System.currentTimeMillis();
        }

        for (Order order : orders) {
            long orderTime = order.getOrderTime();
            if (orderTime <= lastOrderTime) {
                continue; // 已处理订单，跳过
            }

            String symbol = order.getSymbol();
            String side = order.getSide();
            double executedQty = order.getExecutedQty();
            String positionSide = order.getPositionSide();

            if (StringUtils.equalsIgnoreCase(side, Constants.BUY)) {
                // 计算开仓数量
                BigDecimal ratio = leadMarginBalance.divide(availableBalance, 4, RoundingMode.HALF_UP);
                log.info("Processing new order: symbol={}, side={}, executedQty={}, orderTime={}, availableBalance={}，leadMarginBalance={}，ratio={}",
                        symbol, side, executedQty, orderTime, availableBalance, leadMarginBalance, ratio);
                BigDecimal openPositionQty = BigDecimal.valueOf(executedQty)
                        .divide(ratio, 4, RoundingMode.HALF_UP)
                        .multiply(multiplier);
                log.info("placeMarketOrder:symbol={}, side={} , side={}, openPositionQty={}", symbol, side, positionSide, openPositionQty);
                // TODO 如何兼容单向双项持仓
                placeMarketOrder(symbol,side,Constants.LONG,openPositionQty);

                log.info("Calculated open position quantity: {}", openPositionQty);
            } else if (StringUtils.equalsIgnoreCase(side, Constants.SELL)) {
                PositionRisk positionRisk = getPositionRisk(symbol, positionSide);
                log.info("positionRisk:{}", JSON.toJSONString(positionRisk));
                if (positionRisk != null) {
                    log.info("placeMarketOrder:symbol={}, side={} , side={}, openPositionQty={}", symbol, side, positionSide, new BigDecimal(positionRisk.getPositionAmt()));
                    // TODO 如何兼容单向双项持仓
                    placeMarketOrder(symbol,side,Constants.LONG,new BigDecimal(positionRisk.getPositionAmt()));
                }

            }

        }
        return System.currentTimeMillis();
    }
}
