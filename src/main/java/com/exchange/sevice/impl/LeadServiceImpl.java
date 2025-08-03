package com.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.exchange.common.UrlConstants;
import com.exchange.config.BaseurlConfig;
import com.exchange.model.*;
import com.exchange.sevice.LeadService;
import com.exchange.util.HttpUtils;
import com.exchange.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeadServiceImpl implements LeadService {

    @Autowired
    private BaseurlConfig baseurlConfig;

    @Value("${binance.csrftoken}")
    private String csrftoken;

    @Value("${binance.cookie}")
    private String cookie;

    private Map<String,String> headers = new ConcurrentHashMap<>();
    @PostConstruct
    public void initHeaders() {
        headers.put("csrftoken", csrftoken);
        headers.put("Cookie", cookie);
        log.info("Headers initialized: {}", JSON.toJSONString(headers));
    }

    public void updateHeader(String key, String value) {
        headers.put(key, value);
        log.info("Header updated: key={}, value={}", key, value);
    }

    public boolean removeHeader(String key) {
        if (headers.containsKey(key)) {
            headers.remove(key);
            log.info("Header removed: key={}", key);
            return true;
        }
        return false;
    }

    public List<Order> getOrderHistoryList(PortfolioQueryRequest request) {
        try {
            log.info("getOrderHistoryList request:{}", JSON.toJSONString(request));
            String response = HttpUtils.httpPost(baseurlConfig.getWebUrl()+UrlConstants.COPY_TRADE_ORDER_HISTORY, JSON.toJSONString(request),headers);
            log.info("getOrderHistoryList response: {}", response);
            OrderHistory orderHistory = JsonUtils.parseResponse(response, new TypeReference<ApiResponse<OrderHistory>>() {});

            return orderHistory.getList()
                    .stream()
                    .sorted(Comparator.comparingLong(Order::getOrderTime))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal getLeadMarginBalance(String portfolioId) {
        try {
            log.info("getLeadMarginBalance portfolioId:{}", portfolioId);
            String response = HttpUtils.httpGet(baseurlConfig.getWebUrl()+UrlConstants.COPY_TRADE_DETAIL+portfolioId, headers);
            log.info("getLeadMarginBalance response: {}", response);
            LeadPortfolioDetail leadPortfolioDetail = JsonUtils.parseResponse(response, new TypeReference<ApiResponse<LeadPortfolioDetail>>() {
            });
            return Optional.ofNullable(leadPortfolioDetail)
                    .map(LeadPortfolioDetail::getMarginBalance)
                    .map(BigDecimal::new)
                    .orElse(BigDecimal.ZERO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<LeadPosition> getLeadPositions(String portfolioId) {
        try {
            log.info("getLeadPositions portfolioId:{}", portfolioId);
            String response = HttpUtils.httpGet(baseurlConfig.getWebUrl()+UrlConstants.COPY_TRADE_POSITIONS+portfolioId, headers);
            return JsonUtils.parseResponse(response, new TypeReference<ApiResponse<List<LeadPosition>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<LeadPosition> getActivePositions(List<LeadPosition> leadPositions) {
        return leadPositions.stream()
                .filter(LeadPosition::hasPosition)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LeadPosition> getPositionBySymbol(List<LeadPosition> leadPositions, String symbol) {
        return leadPositions.stream()
                .filter(p -> symbol.equalsIgnoreCase(p.getSymbol()))
                .findFirst();
    }


    /**
     * 查询指定交易对和仓位方向的持仓详情
     * @param symbol 合约币种，例如 "ENSUSDT"
     * @param positionSide 仓位方向 "LONG"、"SHORT" 或 "BOTH"
     * @return LeadPosition 持仓对象，找不到返回null
     */
    @Override
    public LeadPosition getPositionBySymbolAndSide(String portfolioId, String symbol, String positionSide) {
        try {
            List<LeadPosition> positions = getLeadPositions(portfolioId);
            if (positions == null || positions.isEmpty()) {
                return null;
            }

            for (LeadPosition pos : positions) {
                if (symbol.equalsIgnoreCase(pos.getSymbol())
                        && positionSide.equalsIgnoreCase(pos.getPositionSide())
                        && pos.hasPosition()) {  // 过滤掉仓位为0的
                    return pos;
                }
            }
        } catch (Exception e) {
            log.error("通过symbol和positionSide获取持仓异常", e);
        }
        return null;
    }



}
