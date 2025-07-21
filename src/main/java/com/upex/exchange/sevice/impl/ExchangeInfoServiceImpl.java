package com.upex.exchange.sevice.impl;

import com.upex.exchange.Api.BinanceApiService;
import com.upex.exchange.model.ExchangeInfoResponse;
import com.upex.exchange.model.SymbolFilter;
import com.upex.exchange.model.SymbolInfo;
import com.upex.exchange.sevice.ExchangeInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ExchangeInfoServiceImpl implements ExchangeInfoService {

    @Autowired
    private BinanceApiService binanceApiService;

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

}
