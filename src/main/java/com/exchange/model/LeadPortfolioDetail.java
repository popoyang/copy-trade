package com.exchange.model;

import lombok.Data;

import java.util.List;

@Data
public class LeadPortfolioDetail {
    private long userId;
    private boolean leadOwner;
    private boolean hasCopy;
    private boolean hasSlotReminder;
    private String leadPortfolioId;
    private String nickname;
    private String nicknameTranslate;
    private String avatarUrl;
    private String status;
    private String description;
    private String descTranslate;
    private int favoriteCount;
    private int currentCopyCount;
    private int maxCopyCount;
    private Integer riskControlMaxCopyCount;
    private Integer finalEffectiveMaxCopyCount;
    private int totalCopyCount;
    private int closeLeadCount;
    private String marginBalance;
    private String initInvestAsset;
    private String futuresType;
    private String aumAmount;
    private String copierPnl;
    private String copierPnlAsset;
    private String profitSharingRate;
    private String unrealizedProfitShareAmount;
    private long startTime;
    private Long endTime;
    private Long closedTime;
    private List<String> tag;
    private boolean positionShow;
    private int mockCopyCount;
    private String sharpRatio;
    private boolean hasMock;
    private int lockPeriod;
    private Long copierLockPeriodTime;
    private Long copierUnlockExpiredTime;
    private String badgeName;
    private long badgeModifyTime;
    private int badgeCopierCount;
    private List<TagItemVo> tagItemVos;
    private boolean feedAgreement;
    private boolean feedShareSwitch;
    private int feedSharePushLimit;
    private double fixedRadioMinCopyUsd;
    private double fixedAmountMinCopyUsd;
    private String portfolioType;
    private String publicLeadPortfolioId;
    private String privateLeadPortfolioId;
    private int inviteCodeCount;
    private boolean enableTradingSignal;
    private boolean enableAddMaxCopier;
    private boolean favorite;
}
