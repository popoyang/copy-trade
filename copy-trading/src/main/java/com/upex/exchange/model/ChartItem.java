package com.upex.exchange.model;

import lombok.Data;

@Data
public class ChartItem {
    private double value;
    private String dataType;
    private long dateTime;
}
