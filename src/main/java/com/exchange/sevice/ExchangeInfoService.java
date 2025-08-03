package com.exchange.sevice;

import com.exchange.model.SymbolFilter;
import com.exchange.model.SymbolInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ExchangeInfoService {
    /**
     * 获取交易规则和交易对
     *
     * @return
     */
    List<SymbolInfo> getSymbolInfo();

    /**
     * 获取对应币对的
     *
     * @param symbol
     * @param symbolInfoList
     * @return SymbolFilter
     */
    SymbolFilter getLotSizeFilter(String symbol, List<SymbolInfo> symbolInfoList);


    /**
     * 获取每个 symbol 的最小下单单位（stepSize）
     */
    Map<String, BigDecimal> getSymbolStepSizeMap();
}
