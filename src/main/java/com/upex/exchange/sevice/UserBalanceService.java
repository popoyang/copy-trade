package com.upex.exchange.sevice;

import com.upex.exchange.model.Order;
import com.upex.exchange.model.OrderResponse;
import com.upex.exchange.model.PositionRisk;

import java.math.BigDecimal;
import java.util.List;

public interface UserBalanceService {
    BigDecimal getAvailableBalance(String asset);


}
