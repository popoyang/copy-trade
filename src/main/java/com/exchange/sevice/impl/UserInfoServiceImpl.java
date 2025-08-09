package com.exchange.sevice.impl;

import com.alibaba.fastjson.JSON;
import com.exchange.api.BinanceApiService;
import com.exchange.config.BinanceAccount;
import com.exchange.config.BinanceAccountsConfig;
import com.exchange.enums.AccountType;
import com.exchange.model.AccountInfo;
import com.exchange.model.BalanceResponse;
import com.exchange.sevice.UserInfoService;
import com.exchange.util.HmacSHA256Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private BinanceApiService binanceApiService;

    @Autowired
    private BinanceAccountsConfig accountsConfig;

    private BinanceAccount getAccount(AccountType accountType) {
        switch(accountType) {
            case SECOND:
                return accountsConfig.getSecond();
            case MAIN:
            default:
                return accountsConfig.getMain();
        }
    }

    @Value("${binance.api.recvWindow:5000}")
    private long recvWindow;

    @Override
    public BigDecimal getAvailableBalance(AccountType accountType,String asset) {
        try {
            //生成签名
            long timestamp  = System.currentTimeMillis();
            String query = "timestamp=" + timestamp;
            String signature = HmacSHA256Utils.sign(query, getAccount(accountType).getSecretKey());

            // 调用获取余额接口
            Call<List<BalanceResponse>> call = binanceApiService.getFuturesBalance(timestamp, signature, getAccount(accountType).getApiKey());
            Response<List<BalanceResponse>> response = call.execute();

            log.info("getAvailableBalance Response: {}", JSON.toJSONString(response.body()));

            if (response.isSuccessful() && response.body() != null) {
                for (BalanceResponse balance : response.body()) {
                    if (asset.equalsIgnoreCase(balance.getAsset())) {
                        return new BigDecimal(balance.getAvailableBalance());
                    }
                }
                log.warn("Asset {} not found in Binance response.", asset);
            } else {
                log.error("Binance response error: {}", response.errorBody() != null ? response.errorBody().string() : "null");
            }
        } catch (Exception e) {
            log.error("Exception while fetching balance: ", e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public AccountInfo getAccountInfo(AccountType accountType) {
        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;

        String signature = HmacSHA256Utils.sign(queryString, getAccount(accountType).getSecretKey());

        try {
            Call<AccountInfo> call = binanceApiService.getAccountInfo(timestamp, recvWindow, signature, getAccount(accountType).getApiKey());
            Response<AccountInfo> response = call.execute();
            if (response.isSuccessful()) {
                AccountInfo info = response.body();
                log.info("AccountInfo: {}", JSON.toJSONString(info));
                return info;
            } else {
                log.error("Failed to get account info: {}", response.errorBody().string());
            }
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
        }
        return null;
    }

    @Override
    public BigDecimal getAvailableMarginBalance(AccountType accountType,String usdt) {
        AccountInfo accountInfo = getAccountInfo(accountType);
        if (accountInfo == null) {
            return BigDecimal.ZERO;
        }
        return accountInfo.getAvailableBalance();
    }


}
