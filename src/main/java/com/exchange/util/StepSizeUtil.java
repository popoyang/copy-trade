package com.exchange.util;

import com.exchange.model.SymbolFilter;
import com.exchange.model.SymbolInfo;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StepSizeUtil {

    // 本地缓存交易对精度，建议在启动时从 exchangeInfo 缓存
    private static final Map<String, Integer> stepSizeMap = new ConcurrentHashMap<>();

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
     * @param value 原始数量
     * @param stepSize 最小单位，如 0.001
     * @return 向下截断后的数量
     */
    public static BigDecimal trimToStepSize(String value, BigDecimal stepSize) {
        if (StringUtils.isBlank(value) || stepSize == null || stepSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("value 和 stepSize 都必须大于0");
        }
        BigDecimal divided = new BigDecimal(value).divide(stepSize, 0, RoundingMode.DOWN); // 整除次数
        return divided.multiply(stepSize);
    }

}

