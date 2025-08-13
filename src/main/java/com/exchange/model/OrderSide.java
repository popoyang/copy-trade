package com.exchange.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderSide {
    private String orderSide;
    private String positionSide;
}
