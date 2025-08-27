package com.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.exchange.common.Constants;
import com.exchange.common.RedisKeyConstants;
import com.exchange.enums.AccountType;
import com.exchange.model.LeadPosition;
import com.exchange.model.OrderSide;
import com.exchange.sevice.*;
import com.exchange.util.StepSizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${account.ratioMultiplier:1}")
    private BigDecimal ratioMultiplier;


    public void syncAndReplicatePositions(String portfolioId) {
        List<LeadPosition> currentPositions = leadService.getLeadPositions(portfolioId);
        List<LeadPosition> activePositions = leadService.getActivePositions(currentPositions);
        log.info("syncAndReplicatePositions active positions: {}", activePositions);

        // 并行处理所有 AccountType
        Arrays.stream(AccountType.values()).parallel().forEach(accountType -> {
            Set<String> currentKeys = new HashSet<>();
            BigDecimal myAvailableMargin = null;
            BigDecimal leadAvailableMargin = null;
            BigDecimal ratio = null;

            for (LeadPosition pos : activePositions) {
                String key = getPositionKey(accountType, pos);
                currentKeys.add(key);

                LeadPosition lastPos = leadPositionRedisTemplate.opsForValue().get(key);
                BigDecimal currentQty = new BigDecimal(pos.getPositionAmount());
                BigDecimal lastQty = lastPos != null ? new BigDecimal(lastPos.getPositionAmount()) : BigDecimal.ZERO;
                BigDecimal diff = calculateDiffBasedOnPositionType(pos,currentQty,lastQty);

                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                    if (myAvailableMargin == null || leadAvailableMargin == null) {
                        myAvailableMargin = userInfoService.getAvailableMarginBalance(accountType, Constants.USDT);
                        leadAvailableMargin = leadService.getLeadMarginBalance(portfolioId);

                        if (leadAvailableMargin.compareTo(BigDecimal.ZERO) == 0) {
                            log.warn("{} leadAvailableMargin为0，跳过后续下单处理", accountType.name());
                            break;
                        }
                        ratio = calculateRatio(accountType, myAvailableMargin, leadAvailableMargin);
                    }

                    BigDecimal myOrderQty = StepSizeUtil.trimToStepSize(pos.getSymbol(), diff.abs().multiply(ratio));

                    if (myOrderQty.compareTo(BigDecimal.ZERO) == 0) {
                        log.info("{} 跳过小于最小下单单位的数量，symbol={} diff={} myOrderQty={}", accountType.name(), pos.getSymbol(), diff, myOrderQty);
                    } else {
                        OrderSide orderSide = getOrderSide(pos.getPositionSide(), currentQty, diff.compareTo(BigDecimal.ZERO) > 0);
                        log.info("{} [复刻下单] symbol={} positionSide={} lastQty={} currentQty={} diff={} 下单数量={} orderDetails={}",
                                accountType.name(), pos.getSymbol(), pos.getPositionSide(), lastQty, currentQty, diff, myOrderQty, JSON.toJSONString(orderSide));

                        retryOrderService.placeMarketOrderWithRetry(accountType, pos.getSymbol(), orderSide.getOrderSide(), orderSide.getPositionSide(), myOrderQty);
                    }
                }

                // 存入 Redis 作为快照
                leadPositionRedisTemplate.opsForValue().set(key, pos);
            }

            handleZeroPositions(accountType, currentKeys, currentPositions);
        });
    }

    private void handleZeroPositions(AccountType accountType,Set<String> currentKeys,List<LeadPosition> currentPositions) {
        Set<String> allKeys = leadPositionRedisTemplate.keys(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + ":" + accountType.name() + "*");
        if (allKeys == null || allKeys.isEmpty()) return;

        for (String redisKey : allKeys) {
            String simpleKey = redisKey.replace(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + ":" + accountType.name(), "");
            if (currentKeys.contains(simpleKey)) continue;

            Boolean isPending = stringRedisTemplate.opsForSet().isMember(RedisKeyConstants.PENDING_CLOSE_SET + ":" + accountType.name(), simpleKey);
            if (Boolean.TRUE.equals(isPending)) {
                log.info("{}仓位 {} 已在等待延迟平仓中，跳过",accountType.name(), simpleKey);
                continue;
            }

            LeadPosition lastPos = leadPositionRedisTemplate.opsForValue().get(redisKey);
            if (lastPos == null) continue;

            LeadPosition latestPos = leadService.getPositionBySymbolAndSide(currentPositions, lastPos.getSymbol(), lastPos.getPositionSide());
            BigDecimal latestQty = latestPos != null ? new BigDecimal(latestPos.getPositionAmount()) : BigDecimal.ZERO;

            // 若最新仓位仍为0或不存在，确认平仓数量执行下单
            if (latestQty.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal myOrderQty = orderService.getMyPositionQuantity(accountType,lastPos.getSymbol(), lastPos.getPositionSide());

                // 根据 stepSize 进行截断
                myOrderQty = StepSizeUtil.trimToStepSize(lastPos.getSymbol(), myOrderQty);

                if (myOrderQty.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("{}[归零平仓处理] symbol={} positionSide={} 我方当前持仓数量={}，将尝试市价平仓",
                            accountType.name(), lastPos.getSymbol(), lastPos.getPositionSide(), myOrderQty);

                    // 加入等待中
                    stringRedisTemplate.opsForSet().add(RedisKeyConstants.PENDING_CLOSE_SET + ":" + accountType.name(), simpleKey);

                    retryOrderService.submitDelayedClose(accountType,
                            lastPos.getSymbol(), lastPos.getPositionSide(), myOrderQty, simpleKey,
                            (k, closed) -> {
                                stringRedisTemplate.opsForSet().remove(RedisKeyConstants.PENDING_CLOSE_SET + ":" + accountType.name(), k);
                                if (closed) {
                                    leadPositionRedisTemplate.delete(RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH + ":" + accountType.name() + k);
                                    log.info("{}延迟平仓完成并删除Redis快照：{}",accountType.name(),  k);
                                } else {
                                    log.warn("{}延迟平仓未完成，保留Redis快照：{}",accountType.name(),  k);
                                }
                            }
                    );
                } else {
                    log.debug("{}归零平仓检查: 我方已无持仓，跳过 symbol={} positionSide={}",accountType.name(),  lastPos.getSymbol(), lastPos.getPositionSide());
                }
            }


        }
    }

    // 新增：计算差异时考虑仓位方向
    private BigDecimal calculateDiffBasedOnPositionType(LeadPosition pos, BigDecimal currentQty, BigDecimal lastQty) {
        if ("SHORT".equalsIgnoreCase(pos.getPositionSide()) || "SELL".equalsIgnoreCase(pos.getPositionSide())) {
            // 对于 SHORT 或 SELL 仓位，确保差异计算时考虑仓位的符号
            currentQty = currentQty.abs();
            lastQty = lastQty.abs();
        }
        return currentQty.subtract(lastQty);
    }


    private OrderSide getOrderSide(String positionSide, BigDecimal positionAmount, boolean isOpen) {
        if ("BOTH".equalsIgnoreCase(positionSide)) {
            if (isOpen) {
                // 当前是开仓
                if (positionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 如果已有多头仓位，应该开空仓（SELL）
                    return new OrderSide("BUY", "LONG");
                } else if (positionAmount.compareTo(BigDecimal.ZERO) < 0) {
                    // 如果已有空头仓位，应该开多仓（BUY）
                    return new OrderSide("SELL", "SHORT");

                } else {
                    // 如果当前没有仓位，可以选择开多仓或空仓（默认开多仓）
                    return new OrderSide("BUY", "LONG");
                }
            } else {
                // 当前是平仓
                if (positionAmount.compareTo(BigDecimal.ZERO) > 0) {
                    return new OrderSide("SELL", "LONG");
                } else if (positionAmount.compareTo(BigDecimal.ZERO) < 0) {
                    // 平空仓（BUY）
                    return new OrderSide("BUY", "SHORT");
                }
            }
        } else {
            // 如果是 LONG 或 SHORT，正常的开仓或平仓操作
            if ("LONG".equalsIgnoreCase(positionSide)) {
                return new OrderSide(isOpen ? "BUY" : "SELL", "LONG");
            } else if ("SHORT".equalsIgnoreCase(positionSide)) {
                return new OrderSide(isOpen ? "SELL" : "BUY", "SHORT");
            }
        }

        // 默认情况（可以根据需求调整）
        return new OrderSide(isOpen ? "BUY" : "SELL", "LONG");
    }


    private String getPositionKey(AccountType accountType, LeadPosition pos) {
        return RedisKeyConstants.LEAD_POSITION_SNAPSHOT_HASH
                + ":"
                + accountType.name() + ":"
                + pos.getSymbol() + "_"
                + pos.getPositionSide();
    }


    public BigDecimal calculateRatio(AccountType accountType, BigDecimal myAvailableMargin, BigDecimal leadAvailableMargin) {
        BigDecimal baseRatio = myAvailableMargin.divide(leadAvailableMargin, 8, RoundingMode.DOWN);
        if (accountType == AccountType.MAIN) {
            return baseRatio;
        } else {
            return baseRatio.multiply(ratioMultiplier);
        }
    }

}