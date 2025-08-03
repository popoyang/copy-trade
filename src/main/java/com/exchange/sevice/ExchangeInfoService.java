package com.exchange.sevice;

import com.exchange.model.SymbolFilter;
import com.exchange.model.SymbolInfo;

import java.util.List;

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
}
