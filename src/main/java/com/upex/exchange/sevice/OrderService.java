package com.upex.exchange.sevice;

import com.upex.exchange.model.Order;
import com.upex.exchange.model.OrderResponse;
import com.upex.exchange.model.PositionRisk;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    OrderResponse placeMarketOrder(String symbol,
                                   String side,
                                   String positionSide,
                                   BigDecimal quantity);

    PositionRisk getPositionRisk(String symbol, String positionSide);

    long processOrders(List<Order> orders, String portfolioId, long lastOrderTime);
}
