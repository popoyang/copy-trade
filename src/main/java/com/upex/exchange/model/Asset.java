package com.upex.exchange.model;

import lombok.Data;

@Data
public class Asset {
    private String asset;
    private boolean marginAvailable;
    private String autoAssetExchange;
}
