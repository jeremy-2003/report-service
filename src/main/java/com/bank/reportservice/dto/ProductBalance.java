package com.bank.reportservice.dto;

import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductBalance {
    private String productId;
    private ProductCategory type;
    private ProductSubType subType;
    private LocalDateTime createdAt;
    private BigDecimal availableBalance;
}
