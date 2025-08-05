package com.exchange.sevice;

import com.exchange.model.LeadPosition;
import com.exchange.model.Order;
import com.exchange.model.PortfolioQueryRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LeadService {


    void updateHeader(String key, String value);

    boolean removeHeader(String key);

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


    /**
     * 获取交易员当前持仓
     * @param portfolioId
     * @return
     */
    List<LeadPosition> getLeadPositions(String portfolioId);

    /**
     * 获取当前持仓
     * @param leadPositions
     * @return
     */
    List<LeadPosition> getActivePositions(List<LeadPosition> leadPositions);

    /**
     * 获取对应币对的仓位
     * @param leadPositions
     * @param symbol
     * @return
     */
    Optional<LeadPosition> getPositionBySymbol(List<LeadPosition> leadPositions, String symbol);

    LeadPosition getPositionBySymbolAndSide(String portfolioId, String symbol, String positionSide);

    LeadPosition getPositionBySymbolAndSide(List<LeadPosition> leadPositions,String symbol, String positionSide);
}
