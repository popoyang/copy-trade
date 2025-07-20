package com.upex.exchange.sevice;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.upex.exchange.common.UrlConstants;
import com.upex.exchange.model.ApiResponse;
import com.upex.exchange.model.Order;
import com.upex.exchange.model.OrderHistory;
import com.upex.exchange.model.PortfolioQueryRequest;
import com.upex.exchange.util.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PortfolioService {

    private Map<String,String> headers = new ConcurrentHashMap<>();


    public List<Order> getOrderHistoryList(PortfolioQueryRequest request) {
        try {
            log.info("getOrderHistoryList request:{}", JSON.toJSONString(request));
            String response = HttpUtils.httpPost(UrlConstants.COPY_TRADE_ORDER_HISTORY, JSON.toJSONString(request),headers);
            log.info("getOrderHistoryList response: {}", response);
            ApiResponse<OrderHistory> apiResponse =
                    JSON.parseObject(response, new TypeReference<ApiResponse<OrderHistory>>() {});
            return apiResponse.getData().getList()
                    .stream()
                    .sorted(Comparator.comparingLong(Order::getOrderTime))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateHeaders(Map<String,String> updateHeaders) {
        log.info("updateUserInfoMap updateHeaders:{}", JSON.toJSONString(updateHeaders));
        headers = updateHeaders;
    }


}
