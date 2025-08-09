package com.exchange.sevice;

import com.exchange.enums.AccountType;
import com.exchange.model.OrderResponse;

import java.math.BigDecimal;

public interface OrderService {

    /**
     * 下市价单
     * @param symbol 币种
     * @param side
     * @param positionSide
     * @param quantity
     * @return
     */
    OrderResponse placeMarketOrder(AccountType accountType,
                                   String symbol,
                                   String side,
                                   String positionSide,
                                   BigDecimal quantity);
    /**
     * 获取仓位数量
     * @param symbol 币种
     * @param positionSide
     * @return 仓位数据
     */
    BigDecimal getMyPositionQuantity(AccountType accountType,String symbol, String positionSide);

}
