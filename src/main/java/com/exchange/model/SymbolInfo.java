package com.exchange.model;

import lombok.Data;

import java.util.List;

@Data
public class SymbolInfo {
    private String symbol;
    private String pair;
    private String contractType;
    private long deliveryDate;
    private long onboardDate;
    private String status;
    private String maintMarginPercent;
    private String requiredMarginPercent;
    private String baseAsset;
    private String quoteAsset;
    private String marginAsset;
    private int pricePrecision;
    private int quantityPrecision;
    private int baseAssetPrecision;
    private int quotePrecision;
    private String underlyingType;
    private List<String> underlyingSubType;
    private int settlePlan;
    private String triggerProtect;
    private List<SymbolFilter> filters;
    private List<String> orderType;
    private List<String> timeInForce;
    private String liquidationFee;
    private String marketTakeBound;
}
