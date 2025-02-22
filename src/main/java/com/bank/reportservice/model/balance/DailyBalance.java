package com.bank.reportservice.model.balance;

import lombok.*;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_balances")
public class DailyBalance {
    @Id
    private String id;
    private String customerId;
    private String productId;
    private String productType;
    private String subType;
    private BigDecimal balance;
    private LocalDateTime date;
}
