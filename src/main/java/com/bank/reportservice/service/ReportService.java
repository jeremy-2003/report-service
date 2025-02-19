package com.bank.reportservice.service;

import com.bank.reportservice.dto.CustomerBalances;
import com.bank.reportservice.dto.ProductBalance;
import com.bank.reportservice.dto.ProductMovement;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {
    private final AccountClientService accountClient;
    private final CreditClientService creditClient;
    private final TransactionClientService transactionClient;
    public Mono<CustomerBalances> getCustomerBalances(String customerId) {
        return Mono.zip(
                accountClient.getAccountsByCustomer(customerId),
                creditClient.getCreditCardsByCustomer(customerId),
                creditClient.getCreditsByCustomer(customerId)
        ).map(tuple -> {
            List<ProductBalance> products = new ArrayList<>();
            // Add account balances
            tuple.getT1().forEach(account -> products.add(
                    ProductBalance.builder()
                            .productId(account.getId())
                            .type(ProductCategory.ACCOUNT)
                            .subType(mapAccountType(account.getAccountType()))
                            .availableBalance(BigDecimal.valueOf(account.getBalance()))
                            .build()
            ));
            // Add credit card balances
            tuple.getT2().forEach(card -> products.add(
                    ProductBalance.builder()
                            .productId(card.getId())
                            .type(ProductCategory.CREDIT_CARD)
                            .subType(mapCreditCardType(card.getCardType()))
                            .availableBalance(card.getAvailableBalance())
                            .build()
            ));
            // Add credit balances
            tuple.getT3().forEach(credit -> products.add(
                    ProductBalance.builder()
                            .productId(credit.getId())
                            .type(ProductCategory.CREDIT)
                            .subType(mapCreditType(credit.getCreditType()))
                            .availableBalance(credit.getRemainingBalance())
                            .build()
            ));
            return CustomerBalances.builder()
                    .customerId(customerId)
                    .products(products)
                    .build();
        });
    }
    public Mono<List<ProductMovement>> getProductMovements(String customerId, String productId) {
        return transactionClient.getTransactionsByCustomerAndProduct(customerId, productId)
                .map(transactions -> transactions.stream()
                        .map(transaction -> ProductMovement.builder()
                                .transactionId(transaction.getId())
                                .date(transaction.getTransactionDate())
                                .type(transaction.getTransactionType())
                                .amount(transaction.getAmount())
                                .productCategory(transaction.getProductCategory())
                                .productSubType(transaction.getProductSubType())
                                .build())
                        .collect(Collectors.toList()));
    }
    // Mapping methods
    private ProductSubType mapAccountType(AccountType type) {
        return switch (type) {
            case SAVINGS -> ProductSubType.SAVINGS;
            case CHECKING -> ProductSubType.CHECKING;
            case FIXED_TERM -> ProductSubType.FIXED_TERM;
        };
    }
    private ProductSubType mapCreditCardType(CreditCardType type) {
        return switch (type) {
            case PERSONAL_CREDIT_CARD -> ProductSubType.PERSONAL_CREDIT_CARD;
            case BUSINESS_CREDIT_CARD -> ProductSubType.BUSINESS_CREDIT_CARD;
        };
    }
    private ProductSubType mapCreditType(CreditType type) {
        return switch (type) {
            case PERSONAL -> ProductSubType.PERSONAL_CREDIT;
            case BUSINESS -> ProductSubType.BUSINESS_CREDIT;
        };
    }
}