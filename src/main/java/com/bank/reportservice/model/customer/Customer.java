package com.bank.reportservice.model.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    private String id;
    private String fullName;
    private String documentNumber;
    private CustomerType customerType;
    private String email;
    private String phone;
    private LocalDateTime createdAd;
    private LocalDateTime modifiedAd;
    private String status;
    //Only for special profiles
    private boolean isVip;
    private boolean isPym;
}
