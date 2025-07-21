package com.upex.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.upex.exchange.common.UrlConstants;
import com.upex.exchange.config.BaseurlConfig;
import com.upex.exchange.model.*;
import com.upex.exchange.sevice.LeadService;
import com.upex.exchange.util.HttpUtils;
import com.upex.exchange.util.JsonUtils;
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


}
