package com.upex.exchange.sevice;

import com.upex.exchange.model.AccountInfo;

import java.math.BigDecimal;

public interface UserInfoService {

    /**
     * 获取对应币种的可用余额
     * @param asset
     * @return
     */
    BigDecimal getAvailableBalance(String asset);

    /**
     * 获取账户信息
     * @return 账户信息
     */
    AccountInfo getAccountInfo();
}
