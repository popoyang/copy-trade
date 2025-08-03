package com.exchange.sevice.impl;

import com.exchange.api.BinanceApiService;
import com.exchange.model.ExchangeInfoResponse;
import com.exchange.model.SymbolFilter;
import com.exchange.model.SymbolInfo;
import com.exchange.sevice.ExchangeInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExchangeInfoServiceImpl implements ExchangeInfoService {

    @Autowired
    private BinanceApiService binanceApiService;

    private static final Map<String, BigDecimal> SYMBOL_STEP_SIZE_MAP = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Map<String, BigDecimal> stepSizeMap = getSymbolStepSizeMap();
            SYMBOL_STEP_SIZE_MAP.clear();
            SYMBOL_STEP_SIZE_MAP.putAll(stepSizeMap);
        } catch (Exception e) {
            log.error("初始化 symbol stepSize 失败", e);
        }
    }

    @Override
    public List<SymbolInfo> getSymbolInfo() {
        try {
            Call<ExchangeInfoResponse> call = binanceApiService.getExchangeInfo();
            Response<ExchangeInfoResponse> response = call.execute();
            if (!response.isSuccessful()) {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                log.error("Failed to fetch getExchangeInfo. Code: {}, Body: {}", response.code(), errorBody);
                return null;
            }
            ExchangeInfoResponse body = response.body();
            if (body != null) {
                return body.getSymbols();
            }

        } catch (Exception e) {
            log.error("Exception while fetching Binance position risk", e);
        }
        return Collections.emptyList();
    }


    @Override
    public SymbolFilter getLotSizeFilter(String symbol, List<SymbolInfo> symbolInfoList) {
        for (SymbolInfo symbolInfo : symbolInfoList) {
            if (symbol.equals(symbolInfo.getSymbol())) {
                for (SymbolFilter filter : symbolInfo.getFilters()) {
                    if ("LOT_SIZE".equals(filter.getFilterType())) {
                        return filter;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 获取每个 symbol 的最小下单单位（stepSize）
     */
    @Override
    public Map<String, BigDecimal> getSymbolStepSizeMap() {
        Map<String, BigDecimal> stepSizeMap = new HashMap<>();

        List<SymbolInfo> symbolInfoList = getSymbolInfo();
        if (symbolInfoList == null || symbolInfoList.isEmpty()) {
            log.warn("symbolInfoList is empty");
            return stepSizeMap;
        }

        for (SymbolInfo symbolInfo : symbolInfoList) {
            String symbol = symbolInfo.getSymbol();
            List<SymbolFilter> filters = symbolInfo.getFilters();
            if (filters == null) continue;

            for (SymbolFilter filter : filters) {
                if ("LOT_SIZE".equalsIgnoreCase(filter.getFilterType())) {
                    BigDecimal stepSize = new BigDecimal(filter.getStepSize());
                    stepSizeMap.put(symbol, stepSize);
                    break;
                }
            }
        }

        return stepSizeMap;
    }

    @Scheduled(fixedRate = 30 * 60 * 1000) // 每 30 分钟刷新一次
    public void refreshStepSizeMap() {
        try {
            SYMBOL_STEP_SIZE_MAP.clear();
            SYMBOL_STEP_SIZE_MAP.putAll(getSymbolStepSizeMap());
            log.info("stepSize 缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新 stepSize 缓存失败", e);
        }
    }

}
