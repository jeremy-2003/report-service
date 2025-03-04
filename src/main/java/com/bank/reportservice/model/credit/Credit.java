package com.bank.reportservice.model.credit;

import com.bank.reportservice.model.creditcard.PaymentStatus;
import lombok.*;
import nonapi.io.github.classgraph.json.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit {
    @Id
    private String id;
    private String customerId;
    private CreditType creditType;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private BigDecimal interestRate;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private PaymentStatus paymentStatus;
    private CreditStatus creditStatus;
    private LocalDateTime nextPaymentDate;
    private BigDecimal minimumPayment;
}
