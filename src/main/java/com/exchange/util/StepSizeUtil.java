package com.exchange.util;

import com.exchange.model.SymbolFilter;
import com.exchange.model.SymbolInfo;
import com.exchange.sevice.ExchangeInfoService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class StepSizeUtil {

    // 本地缓存交易对精度，建议在启动时从 exchangeInfo 缓存
    private static final Map<String, Integer> stepSizeMap = new ConcurrentHashMap<>();
    // 这个缓存可以在服务启动时由 ExchangeInfoService 初始化，也可以在第一次调用时懒加载
    private static volatile Map<String, BigDecimal> symbolStepSizeMap = new ConcurrentHashMap<>();

    private static ExchangeInfoService exchangeInfoService;

    public static void setExchangeInfoService(ExchangeInfoService service) {
        exchangeInfoService = service;
        refreshSymbolStepSizeMap();
    }

    public static void refreshSymbolStepSizeMap() {
        if (exchangeInfoService != null) {
            try {
                symbolStepSizeMap = exchangeInfoService.getSymbolStepSizeMap();
            } catch (Exception e) {
                throw new RuntimeException("加载 symbolStepSizeMap 失败", e);
            }
        }
    }

    public static void putStepSize(String symbol, int precision) {
        stepSizeMap.put(symbol, precision);
    }

    public static BigDecimal truncate(String symbol, BigDecimal value) {
        Integer precision = stepSizeMap.get(symbol);
        if (precision == null) {
            // 未配置默认4位，或抛异常按需处理
            precision = 4;
        }
        return value.setScale(precision, RoundingMode.DOWN);
    }

    public SymbolFilter getLotSizeFilter(String symbol, List<SymbolInfo> symbolInfoList) {
        for (SymbolInfo symbolInfo : symbolInfoList) {
            if (symbol.equals(symbolInfo.getSymbol())) {
                for (SymbolFilter filter : symbolInfo.getFilters()) {
                    if ("LOT_SIZE".equals(filter.getFilterType())) {
                        int precision = getPrecisionFromStepSize(filter.getStepSize());
                        stepSizeMap.put(symbol, precision);
                    }
                }
            }
        }
        return null;
    }

    private static int getPrecisionFromStepSize(String stepSize) {
        return stepSize.indexOf('1') - 2;
    }

    /**
     * 将数量截断为符合最小下单单位的值（不四舍五入）
     * @param symbol 交易对标识符，如 "BTCUSDT"
     * @param value 需要截断的数量
     * @return 向下截断为符合最小下单单位的数量
     */
//    public static BigDecimal trimToStepSize(String symbol, BigDecimal value) {
//        BigDecimal stepSize = getStepSize(symbol);
//        log.info("trimToStepSize symbol:{},stepSize:{},value:{}", symbol, stepSize, value);
//        if (stepSize == null || stepSize.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("stepSize 无效: " + stepSize);
//        }
//        BigDecimal divided = value.divide(stepSize, 0, RoundingMode.DOWN);
//        return divided.multiply(stepSize);
//    }
    public static BigDecimal trimToStepSize(String symbol, BigDecimal value) {
        BigDecimal stepSize = getStepSize(symbol);
        log.info("trimToStepSize symbol:{}, stepSize:{}, value:{}", symbol, stepSize, value);
        if (stepSize == null || stepSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("stepSize 无效: " + stepSize);
        }

        BigDecimal divided = value.divide(stepSize, 0, RoundingMode.DOWN);
        BigDecimal result = divided.multiply(stepSize);

        // 如果截断后小于stepSize，返回stepSize
        if (result.compareTo(stepSize) < 0) {
            return stepSize;
        }
        return result;
    }


    /***
     * 获取某个交易对的最小下单单位（stepSize）
     * @param symbol 交易对标识符，如 "BTCUSDT"
     * @return
     */
    public static BigDecimal getStepSize(String symbol) {
        BigDecimal stepSize = symbolStepSizeMap.get(symbol);
        if (stepSize == null && exchangeInfoService != null) {
            // 缓存中没有，尝试刷新一次
            refreshSymbolStepSizeMap();
            stepSize = symbolStepSizeMap.get(symbol);
        }

        if (stepSize == null) {
            throw new IllegalArgumentException("无法获取 symbol 的最小下单单位: " + symbol);
        }

        return stepSize;
    }

}

