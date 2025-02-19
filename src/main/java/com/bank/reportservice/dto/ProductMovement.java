package com.bank.reportservice.dto;

import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import com.bank.reportservice.model.transaction.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductMovement {
    private String transactionId;
    private LocalDateTime date;
    private BigDecimal amount;
    private ProductCategory productCategory;
    private ProductSubType productSubType;
    private TransactionType type;
}
