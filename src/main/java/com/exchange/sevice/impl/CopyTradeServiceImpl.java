package com.exchange.sevice.impl;

import com.exchange.common.Constants;
import com.exchange.common.RedisKeyConstants;
import com.exchange.model.LeadPosition;
import com.exchange.sevice.*;
import com.exchange.util.StepSizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class CopyTradeServiceImpl implements CopyTradeService {

    @Autowired
    private LeadService leadService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RetryOrderService retryOrderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate<String, LeadPosition> leadPositionRedisTemplate;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;


    public void syncAndReplicatePositions(String portfolioId) {
        List<LeadPosition> currentPositions = leadService.getLeadPositions(portfolioId);
        List<LeadPosition> activePositions = leadService.getActivePositions(currentPositions);
        log.info("syncAndReplicatePositions active positions: {}", activePositions);

        Set<String> currentKeys = new HashSet<>();

        BigDecimal myAvailableMargin = null;
        BigDecimal leadAvailableMargin = null;
        BigDecimal ratio = null;

        for (LeadPosition pos : activePositions) {
            String key = getPositionKey(pos);
            currentKeys.add(key);

            LeadPosition lastPos = leadPositionRedisTemplate.opsForValue().get(key);
            BigDecimal currentQty = new BigDecimal(pos.getPositionAmount());
            BigDecimal lastQty = lastPos != null ? new BigDecimal(lastPos.getPositionAmount()) : BigDecimal.ZERO;
            BigDecimal diff = currentQty.subtract(lastQty);

            if (diff.compareTo(BigDecimal.ZERO) != 0) {
                if (myAvailableMargin == null || leadAvailableMargin == null) {
                    myAvailableMargin = userInfoService.getAvailableMarginBalance(Constants.USDT);
                    leadAvailableMargin = leadService.getLeadMarginBalance(portfolioId);

                    if (leadAvailableMargin.compareTo(BigDecimal.ZERO) == 0) {
                        log.warn("leadAvailableMargin为0，跳过后续下单处理");
                        break;
                    }
                    ratio = myAvailableMargin.divide(leadAvailableMargin, 8, RoundingMode.DOWN);
                }

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

            // 存入 Redis 作为快照
            leadPositionRedisTemplate.opsForValue().set(key, pos);
        }

        handleZeroPositions(currentKeys,currentPositions);
    }

    private void handleZeroPositions(Set<String> currentKeys,List<LeadPosition> currentPositions) {
        Set<String> allKeys = leadPositionRedisTemplate.keys(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + "*");
        if (allKeys == null || allKeys.isEmpty()) return;

        for (String redisKey : allKeys) {
            String simpleKey = redisKey.replace(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH, "");
            if (currentKeys.contains(simpleKey)) continue;

            Boolean isPending = stringRedisTemplate.opsForSet().isMember(RedisKeyConstants.PENDING_CLOSE_SET, simpleKey);
            if (Boolean.TRUE.equals(isPending)) {
                log.info("仓位 {} 已在等待延迟平仓中，跳过", simpleKey);
                continue;
            }

            LeadPosition lastPos = leadPositionRedisTemplate.opsForValue().get(redisKey);
            if (lastPos == null) continue;

            LeadPosition latestPos = leadService.getPositionBySymbolAndSide(currentPositions, lastPos.getSymbol(), lastPos.getPositionSide());
            BigDecimal latestQty = latestPos != null ? new BigDecimal(latestPos.getPositionAmount()) : BigDecimal.ZERO;

            // 若最新仓位仍为0或不存在，确认平仓数量执行下单
            if (latestQty.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal myOrderQty = orderService.getMyPositionQuantity(lastPos.getSymbol(), lastPos.getPositionSide());

                // 根据 stepSize 进行截断
                myOrderQty = StepSizeUtil.trimToStepSize(lastPos.getSymbol(), myOrderQty);

                if (myOrderQty.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("[归零平仓处理] symbol={} positionSide={} 我方当前持仓数量={}，将尝试市价平仓",
                            lastPos.getSymbol(), lastPos.getPositionSide(), myOrderQty);

                    // 加入等待中
                    stringRedisTemplate.opsForSet().add(RedisKeyConstants.PENDING_CLOSE_SET, simpleKey);

                    retryOrderService.submitDelayedClose(
                            lastPos.getSymbol(), lastPos.getPositionSide(), myOrderQty, simpleKey,
                            (k, closed) -> {
                                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.PENDING_CLOSE_SET, k);
                                if (closed) {
                                    leadPositionRedisTemplate.delete(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + k);
                                    log.info("延迟平仓完成并删除Redis快照：{}", k);
                                } else {
                                    log.warn("延迟平仓未完成，保留Redis快照：{}", k);
                                }
                            }
                    );
                } else {
                    log.debug("归零平仓检查: 我方已无持仓，跳过 symbol={} positionSide={}", lastPos.getSymbol(), lastPos.getPositionSide());
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
        return isOpen ? "BUY" : "SELL";
    }

    private String getPositionKey(LeadPosition pos) {
        return RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + pos.getSymbol() + "_" + pos.getPositionSide();
    }
}