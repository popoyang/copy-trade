package com.exchange.sevice;

@FunctionalInterface
public interface ClosePositionCallback {
    /**
     * @param key position key (symbol+side)
     * @param closed true 表示实际执行了市价平仓，false 表示取消（仓位没归零）
     */
    void onCloseComplete(String key, boolean closed);
}

