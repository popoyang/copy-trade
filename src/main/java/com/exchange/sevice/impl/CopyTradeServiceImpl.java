package com.exchange.sevice.impl;

import com.exchange.common.Constants;
import com.exchange.model.LeadPosition;
import com.exchange.sevice.CopyTradeService;
import com.exchange.sevice.LeadService;
import com.exchange.sevice.RetryOrderService;
import com.exchange.sevice.UserInfoService;
import com.exchange.util.StepSizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CopyTradeServiceImpl implements CopyTradeService {

    @Autowired
    private LeadService leadService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RetryOrderService retryOrderService;

    // 缓存上一次的仓位快照，key格式：symbol_positionSide
    private final Map<String, LeadPosition> lastPositionSnapshots = new HashMap<>();

    private final Set<String> pendingCloseSet = ConcurrentHashMap.newKeySet();

    public void syncAndReplicatePositions(String portfolioId) {
        List<LeadPosition> currentPositions = leadService.getLeadPositions(portfolioId);
        List<LeadPosition> activePositions = leadService.getActivePositions(currentPositions);

        Set<String> currentKeys = new HashSet<>();

        BigDecimal myAvailableMargin = userInfoService.getAvailableBalance(Constants.USDT);
        BigDecimal leadAvailableMargin = leadService.getLeadMarginBalance(portfolioId);

        for (LeadPosition pos : activePositions) {
            String key = getPositionKey(pos);
            currentKeys.add(key);

            LeadPosition lastPos = lastPositionSnapshots.get(key);
            BigDecimal currentQty = new BigDecimal(pos.getPositionAmount());
            BigDecimal lastQty = lastPos != null ? new BigDecimal(lastPos.getPositionAmount()) : BigDecimal.ZERO;
            BigDecimal diff = currentQty.subtract(lastQty);

            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ratio = myAvailableMargin.divide(leadAvailableMargin, 8, BigDecimal.ROUND_DOWN);
                BigDecimal myOrderQty = StepSizeUtil.trimToStepSize(pos.getSymbol(), diff.abs().multiply(ratio));

                if (myOrderQty.compareTo(BigDecimal.ZERO) == 0) {
                    log.info("跳过小于最小下单单位的数量，symbol={} diff={} myOrderQty={}", pos.getSymbol(), diff, myOrderQty);
                } else {
                    String side = getOrderSide(pos.getPositionSide(), diff.compareTo(BigDecimal.ZERO) > 0);
                    log.info("[复刻下单] symbol={} positionSide={} lastQty={} currentQty={} diff={} 下单数量={}",
                            pos.getSymbol(), pos.getPositionSide(), lastQty, currentQty, diff, myOrderQty);

                    retryOrderService.placeMarketOrderWithRetry(
                            pos.getSymbol(), side, pos.getPositionSide(), myOrderQty);
                }
            }

            // 不管是否下单，都更新快照
            lastPositionSnapshots.put(key, pos);
        }

        // 处理归零且未出现在本次快照的仓位，延迟平仓等
        handleZeroPositions(currentKeys, myAvailableMargin, leadAvailableMargin);
    }

    private void handleZeroPositions(Set<String> currentKeys, BigDecimal myMargin, BigDecimal leadMargin) {
        for (String key : lastPositionSnapshots.keySet()) {
            if (currentKeys.contains(key)) continue;

            //若已在等待平仓中，跳过
            if (pendingCloseSet.contains(key)) {
                log.info("仓位 {} 已在等待延迟平仓中，跳过", key);
                continue;
            }

            LeadPosition lastPos = lastPositionSnapshots.get(key);
            BigDecimal quantity = new BigDecimal(lastPos.getPositionAmount());
            if (quantity.compareTo(BigDecimal.ZERO) != 0 && leadMargin.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal myOrderQty = StepSizeUtil.trimToStepSize(
                        lastPos.getSymbol(),
                        quantity.multiply(myMargin.divide(leadMargin, 8, RoundingMode.DOWN))
                );

                if (myOrderQty.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("[归零平仓处理] symbol={} positionSide={} 原持仓数量={} 下单数量={}",
                            lastPos.getSymbol(), lastPos.getPositionSide(), quantity, myOrderQty);

                    // 加入等待列表
                    pendingCloseSet.add(key);

                    // 提交延迟任务，延迟中完成状态清理
                    retryOrderService.submitDelayedClose(
                            lastPos.getSymbol(), lastPos.getPositionSide(), myOrderQty,key,(k, closed) -> {
                                // 始终移除等待集合
                                pendingCloseSet.remove(k);
                                // 只有在真正执行了平仓，才移除 snapshot
                                if (closed) {
                                    lastPositionSnapshots.remove(k);
                                    log.info("延迟平仓完成并移除快照：{}", k);
                                } else {
                                    log.info("延迟平仓被取消，快照保留：{}", k);
                                }
                            }
                    );

                }
            }
        }
    }


    private String getOrderSide(String positionSide, boolean isOpen) {
        if ("LONG".equalsIgnoreCase(positionSide)) {
            return isOpen ? "BUY" : "SELL";
        } else if ("SHORT".equalsIgnoreCase(positionSide)) {
            return isOpen ? "SELL" : "BUY";
        }
        // 单向持仓BOTH场景，默认以 diff 正负判断买卖方向
        return isOpen ? "BUY" : "SELL";
    }

    private String getPositionKey(LeadPosition pos) {
        return pos.getSymbol() + "_" + pos.getPositionSide();
    }
}
