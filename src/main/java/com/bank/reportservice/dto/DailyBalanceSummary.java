package com.bank.reportservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyBalanceSummary {
    private String productId;
    private String productType;
    private String subType;
    private BigDecimal averageBalance;
}
