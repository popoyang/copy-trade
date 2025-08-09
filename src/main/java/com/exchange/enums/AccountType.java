package com.exchange.enums;

public enum AccountType {
    MAIN("main"),
    SECOND("second");

    private final String code;

    AccountType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AccountType code: " + code);
    }
}

