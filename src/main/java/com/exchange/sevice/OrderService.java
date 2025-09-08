package com.exchange.sevice;

import com.exchange.enums.AccountType;
import com.exchange.model.OrderResponse;
import com.exchange.model.PositionRisk;

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


    /**
     * 获取对应仓位
     * @param accountType
     * @param symbol
     * @param positionSide
     * @return
     */
    PositionRisk getMyPositionRisk(AccountType accountType, String symbol, String positionSide);

    /**
     * 计算我的持仓盈亏率（PNL Ratio）
     * @param accountType
     * @param symbol
     * @param positionSide
     * @return
     */
    BigDecimal getMyPositionPnlRatio(AccountType accountType,String symbol, String positionSide);
}
