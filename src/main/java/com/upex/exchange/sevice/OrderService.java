package com.upex.exchange.sevice;

import com.upex.exchange.model.Order;
import com.upex.exchange.model.OrderResponse;
import com.upex.exchange.model.PositionRisk;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    /**
     * 下市价单
     * @param symbol 币种
     * @param side
     * @param positionSide
     * @param quantity
     * @return
     */
    OrderResponse placeMarketOrder(String symbol,
                                   String side,
                                   String positionSide,
                                   BigDecimal quantity);

    /**
     * 获取仓位数据
     * @param symbol 币种
     * @param positionSide
     * @return 仓位数据
     */
    PositionRisk getPositionRisk(String symbol, String positionSide);

    /**
     * 处理订单数据
     * @param orders 订单
     * @param portfolioId 点单员 portfolioId
     * @param lastOrderTime 最新订单时间
     * @return 最新订单时间
     */
    long processOrders(List<Order> orders, String portfolioId, long lastOrderTime);
}
