package com.bank.reportservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerBalances {
    private String customerId;
    private List<ProductBalance> products;
}
