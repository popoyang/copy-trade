package com.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.exchange.api.BinanceApiService;
import com.exchange.config.BinanceAccount;
import com.exchange.config.BinanceAccountsConfig;
import com.exchange.enums.AccountType;
import com.exchange.model.*;
import com.exchange.sevice.ExchangeInfoService;
import com.exchange.sevice.LeadService;
import com.exchange.sevice.OrderService;
import com.exchange.sevice.UserInfoService;
import com.exchange.util.HmacSHA256Utils;
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
    private UserInfoService userInfoService;

    @Autowired
    private ExchangeInfoService exchangeInfoService;

    @Autowired
    private BinanceAccountsConfig accountsConfig;

    @Value("${binance.api.recvWindow:3000}")
    private long recvWindow;

    @Value("${binance.api.multiplier:5}")
    private BigDecimal multiplier;

    private BinanceAccount getAccount(AccountType accountType) {
        switch(accountType) {
//            case SECOND:
//                return accountsConfig.getSecond();
            case MAIN:
            default:
                return accountsConfig.getMain();
        }
    }


    @Override
    public OrderResponse placeMarketOrder(
            AccountType accountType,
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

            String signature = HmacSHA256Utils.sign(queryString, getAccount(accountType).getSecretKey());

            Call<OrderResponse> call = binanceApiService.placeOrder(
                    symbol,
                    side,
                    positionSide,
                    "MARKET",
                    quantity.stripTrailingZeros().toPlainString(),
                    serverTime,
                    signature,
                    recvWindow,
                    getAccount(accountType).getApiKey()
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
    public BigDecimal getMyPositionQuantity(AccountType accountType,String symbol, String positionSide) {
        if (StringUtils.isBlank(symbol) || StringUtils.isBlank(positionSide)) {
            log.warn("getMyPositionQuantity Invalid parameters: symbol or positionSide is blank.");
            return null;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;
            String signature = HmacSHA256Utils.sign(queryString, getAccount(accountType).getSecretKey());

            log.info("getMyPositionQuantity Fetching position risk for symbol: {}, positionSide: {}", symbol, positionSide);

            Call<List<PositionRisk>> call = binanceApiService.getPositionRisk(timestamp, recvWindow, signature, getAccount(accountType).getApiKey());
            Response<List<PositionRisk>> response = call.execute();

            if (!response.isSuccessful()) {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                log.error("getMyPositionQuantity Failed to fetch position risk. Code: {}, Body: {}", response.code(), errorBody);
                return BigDecimal.ZERO;
            }

            List<PositionRisk> positionRisks = response.body();
            if (positionRisks == null || positionRisks.isEmpty()) {
                return BigDecimal.ZERO;
            }

            for (PositionRisk positionRisk : positionRisks) {
                if (symbol.equals(positionRisk.getSymbol())) {
                    return new BigDecimal(positionRisk.getPositionAmt()).abs(); // 返回绝对值
                }
            }


        } catch (Exception e) {
            log.error("Exception while fetching Binance position risk", e);
        }

        return BigDecimal.ZERO;
    }

    @Override
    public PositionRisk getMyPositionRisk(AccountType accountType,String symbol, String positionSide) {
        if (StringUtils.isBlank(symbol)) {
            log.warn("getMyPositionRisk Invalid parameters: symbol or positionSide is blank.");
            return null;
        }

        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;
            String signature = HmacSHA256Utils.sign(queryString, getAccount(accountType).getSecretKey());

            log.info("getMyPositionRisk Fetching position risk for symbol: {}, positionSide: {}", symbol, positionSide);

            Call<List<PositionRisk>> call = binanceApiService.getPositionRisk(timestamp, recvWindow, signature, getAccount(accountType).getApiKey());
            Response<List<PositionRisk>> response = call.execute();

            if (!response.isSuccessful()) {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                log.error("getMyPositionRisk Failed to fetch position risk. Code: {}, Body: {}", response.code(), errorBody);
                return null;
            }

            List<PositionRisk> positionRisks = response.body();
            if (positionRisks == null || positionRisks.isEmpty()) {
                return null;
            }

            for (PositionRisk positionRisk : positionRisks) {
                if (symbol.equals(positionRisk.getSymbol())) {
                    return positionRisk;
                }
            }


        } catch (Exception e) {
            log.error("Exception while fetching Binance position risk", e);
        }

        return null;
    }


    /**
     * 计算我的持仓盈亏率（PNL Ratio）
     * @param accountType
     * @param symbol
     * @param positionSide
     * @return
     */
    public BigDecimal getMyPositionPnlRatio(AccountType accountType,String symbol, String positionSide) {
        PositionRisk position = getMyPositionRisk(accountType, symbol, positionSide);
        if (position == null
                || position.getEntryPrice() == null
                || position.getMarkPrice() == null
                || position.getPositionAmt() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal entryPrice = new BigDecimal(position.getEntryPrice());
        BigDecimal markPrice = new BigDecimal(position.getMarkPrice());
        BigDecimal posAmt = new BigDecimal(position.getPositionAmt());

        if (entryPrice.compareTo(BigDecimal.ZERO) == 0
                || posAmt.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 盈亏 = (标记价格 - 开仓均价) * 持仓数量
        // 仓位价值 = 开仓均价 * |持仓数量|
        BigDecimal pnl = markPrice.subtract(entryPrice).multiply(posAmt);
        BigDecimal positionValue = entryPrice.multiply(posAmt.abs());

        // 盈亏率 = 盈亏 / 仓位价值
        return pnl.divide(positionValue, 8, RoundingMode.HALF_UP);
    }

}
