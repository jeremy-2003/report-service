package com.bank.reportservice.dto;

import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductBalance {
    private String productId;
    private ProductCategory type;
    private ProductSubType subType;
    private BigDecimal availableBalance;
}
