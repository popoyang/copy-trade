package com.upex.exchange.sevice;

import com.upex.exchange.model.*;
import java.math.BigDecimal;
import java.util.List;

public interface LeadService {

    /**
     * 获取对应带单员的最新操作记录
     * @param request
     * @return
     */
    List<Order> getOrderHistoryList(PortfolioQueryRequest request);

    /**
     * 获取带单员的保证金
     * @param portfolioId
     * @return
     */
    BigDecimal getLeadMarginBalance(String portfolioId);


}
