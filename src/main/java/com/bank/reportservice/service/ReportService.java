package com.bank.reportservice.service;

import com.bank.reportservice.dto.*;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import com.bank.reportservice.model.transaction.Transaction;
import com.bank.reportservice.repository.DailyBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {
    private final AccountClientService accountClient;
    private final CreditClientService creditClient;
    private final TransactionClientService transactionClient;
    private final DailyBalanceRepository dailyBalanceRepository;
    public ReportService(AccountClientService accountClient,
                         CreditClientService creditClient,
                         TransactionClientService transactionClient,
                         DailyBalanceRepository dailyBalanceRepository){
        this.accountClient = accountClient;
        this.creditClient = creditClient;
        this.transactionClient = transactionClient;
        this.dailyBalanceRepository = dailyBalanceRepository;
    }

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
    public Mono<List<DailyBalanceSummary>> getMonthlyBalanceSummary(String customerId) {
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1);
        LocalDateTime today = LocalDateTime.now();
        log.info("Finding balances for customer {} between {} and {}", customerId, firstDayOfMonth, today);
        return dailyBalanceRepository.findByCustomerIdAndDateBetween(customerId, firstDayOfMonth, today)
                .collectList()
                .map(this::calculateAverageBalances)
                .doOnNext(list -> log.info("Fetched {} balance summaries", list.size()))
                .doOnError(e -> log.error("Error fetching balance summaries", e));
    }
    private List<DailyBalanceSummary> calculateAverageBalances(List<DailyBalance> balances) {
        if (balances.isEmpty()) {
            return Collections.emptyList(); // Retorna lista vacía si no hay balances
        }
        Map<String, List<DailyBalance>> balancesByProduct = balances.stream()
                .collect(Collectors.groupingBy(DailyBalance::getProductId));
        return balancesByProduct.entrySet().stream()
                .map(entry -> {
                    List<DailyBalance> productBalances = entry.getValue();
                    BigDecimal totalBalance = productBalances.stream()
                            .map(d -> d.getBalance() != null ? d.getBalance() : BigDecimal.ZERO) // Manejo de null
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal averageBalance = productBalances.isEmpty()
                            ? BigDecimal.ZERO // Evita división por cero
                            : totalBalance.divide(BigDecimal.valueOf(productBalances.size()), 2, RoundingMode.HALF_UP);
                    DailyBalance firstBalance = productBalances.get(0); // Evita IndexOutOfBoundsException
                    return new DailyBalanceSummary(
                            entry.getKey(),
                            firstBalance.getProductType(),
                            firstBalance.getSubType(),
                            averageBalance
                    );
                })
                .collect(Collectors.toList());
    }
    public Mono<BaseResponse<List<CategorySummary>>> fetchTransactionSummaryByDate(LocalDate startDate, LocalDate endDate) {
        return transactionClient.getTransactionsByDate(startDate, endDate)
                .flatMap(transactions -> {
                    Map<ProductCategory, List<Transaction>> transactionsByCategory = Arrays.stream(ProductCategory.values())
                            .collect(Collectors.toMap(category -> category, category -> new ArrayList<>()));

                    if (transactions != null && !transactions.isEmpty()) {
                        transactions.stream()
                                .filter(transaction -> transaction != null) // Add null check here
                                .forEach(transaction -> {
                                    ProductCategory category = transaction.getProductCategory();
                                    transactionsByCategory.get(category).add(transaction);
                                });
                    }

                    List<CategorySummary> categorySummaries = transactionsByCategory.entrySet().stream()
                            .map(entry -> {
                                int quantity = entry.getValue().size();
                                double totalCommissions = entry.getValue().stream()
                                        .filter(transaction -> transaction != null && transaction.getCommissions() != null) // Add null check here
                                        .map(Transaction::getCommissions)
                                        .mapToDouble(BigDecimal::doubleValue)
                                        .sum();
                                return new CategorySummary(entry.getKey().toString(), quantity, totalCommissions);
                            })
                            .collect(Collectors.toList());

                    return Mono.just(BaseResponse.<List<CategorySummary>>builder()
                            .status(HttpStatus.OK.value())
                            .message("Transactions retrieved successfully")
                            .data(categorySummaries)
                            .build());
                });
    }



}