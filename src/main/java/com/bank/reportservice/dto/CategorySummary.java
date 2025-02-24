package com.bank.reportservice.dto;
import lombok.*;

@Data
@AllArgsConstructor
public class CategorySummary {
    private String category;
    private int quantity;
    private double commissions;
}
