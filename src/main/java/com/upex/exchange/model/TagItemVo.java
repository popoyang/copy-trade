package com.upex.exchange.model;

import lombok.Data;

import java.util.Map;

@Data
public class TagItemVo {
    private String tagName;
    private String tagLangKey;
    private String descLangKey;
    private Map<String, String> describeParams;
    private String enDescribe;
    private String cnDescribe;
    private int sort;
}
