package com.upex.exchange.Api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import com.upex.exchange.model.BalanceResponse;
import com.upex.exchange.model.OrderResponse;
import com.upex.exchange.model.PositionRisk;
import com.upex.exchange.model.ServerTimeResponse;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

@RetrofitClient(baseUrl = "${binance.apiUrl}")
public interface BinanceApiService {

    @GET("/fapi/v3/balance")
    Call<List<BalanceResponse>> getFuturesBalance(
            @Query("timestamp") long timestamp,
            @Query("signature") String signature,
            @Header("X-MBX-APIKEY") String apiKey
    );

    @POST("/fapi/v1/order")
    @FormUrlEncoded
    Call<OrderResponse> placeOrder(
            @Field("symbol") String symbol,
            @Field("side") String side,
            @Field("positionSide") String positionSide,
            @Field("type") String type,
            @Field("quantity") String quantity,
            @Field("timestamp") long timestamp,
            @Field("signature") String signature,
            @Field("recvWindow") Long recvWindow,
            @Header("X-MBX-APIKEY") String apiKey
    );

    @GET("/fapi/v2/positionRisk")
    Call<List<PositionRisk>> getPositionRisk(
            @Query("timestamp") long timestamp,
            @Query("recvWindow") Long recvWindow,
            @Query("signature") String signature,
            @Header("X-MBX-APIKEY") String apiKey
    );


    @GET("/api/v3/time")
    Call<ServerTimeResponse> getServerTime();
}
