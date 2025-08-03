package com.exchange.model;

import lombok.Data;

import java.util.List;

@Data
public class OrderHistory {
    private String indexValue;
    private int total;
    private List<Order> list;
}
