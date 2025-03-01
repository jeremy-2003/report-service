package com.bank.reportservice.service;

import com.bank.reportservice.dto.*;
import com.bank.reportservice.model.account.Account;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCard;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.debitcard.DebitCard;
import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import com.bank.reportservice.model.transaction.Transaction;
import com.bank.reportservice.repository.DailyBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

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
    private final DebitCardClientService debitCardClientService;
    public ReportService(AccountClientService accountClient,
                         CreditClientService creditClient,
                         TransactionClientService transactionClient,
                         DailyBalanceRepository dailyBalanceRepository,
                         DebitCardClientService debitCardClientService) {
        this.accountClient = accountClient;
        this.creditClient = creditClient;
        this.transactionClient = transactionClient;
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.debitCardClientService = debitCardClientService;
    }
    public Mono<CustomerBalances> getResumeByProductAndUserAndDates(String typeProduct, String customerId, LocalDate startDate, LocalDate endDate) {
        return getCustomerBalances(customerId)
                .map(customerBalances -> filterBalancesByTypeAndDates(customerBalances, typeProduct, startDate, endDate));
    }
    public Mono<CustomerBalances> getCustomerBalances(String customerId) {
        return Mono.zip(
                accountClient.getAccountsByCustomer(customerId),
                creditClient.getCreditCardsByCustomer(customerId),
                creditClient.getCreditsByCustomer(customerId),
                debitCardClientService.getDebitCardsByCustomer(customerId)
        ).flatMap(tuple -> mapToCustomerBalances(customerId, tuple));
    }

    private Mono<CustomerBalances> mapToCustomerBalances(String customerId, Tuple4<List<Account>, List<CreditCard>, List<Credit>, List<DebitCard>> tuple) {
        List<ProductBalance> products = new ArrayList<>();

        tuple.getT1().forEach(account -> products.add(
                ProductBalance.builder()
                        .productId(account.getId())
                        .type(ProductCategory.ACCOUNT)
                        .subType(mapAccountType(account.getAccountType()))
                        .availableBalance(BigDecimal.valueOf(account.getBalance()))
                        .createdAt(account.getCreatedAt())
                        .build()
        ));

        tuple.getT2().forEach(card -> products.add(
                ProductBalance.builder()
                        .productId(card.getId())
                        .type(ProductCategory.CREDIT_CARD)
                        .subType(mapCreditCardType(card.getCardType()))
                        .availableBalance(card.getAvailableBalance())
                        .createdAt(card.getCreatedAt())
                        .build()
        ));

        tuple.getT3().forEach(credit -> products.add(
                ProductBalance.builder()
                        .productId(credit.getId())
                        .type(ProductCategory.CREDIT)
                        .subType(mapCreditType(credit.getCreditType()))
                        .availableBalance(credit.getRemainingBalance())
                        .createdAt(credit.getCreatedAt())
                        .build()
        ));

        return Flux.fromIterable(tuple.getT4())
                .flatMap(debitCard -> accountClient.getAccountById(debitCard.getPrimaryAccountId())
                        .map(account -> {
                            BigDecimal accountBalance = BigDecimal.valueOf(account.getBalance());
                            return ProductBalance.builder()
                                    .productId(debitCard.getId())
                                    .type(ProductCategory.DEBIT_CARD)
                                    .subType(null)
                                    .availableBalance(accountBalance)
                                    .createdAt(debitCard.getCreatedAt())
                                    .build();
                        })
                        .doOnError(error -> System.err.println("Error obtaining the account: " + error.getMessage()))
                        .onErrorResume(error -> Mono.empty())
                )
                .collectList()
                .map(debitCardBalances -> {
                    products.addAll(debitCardBalances);
                    return CustomerBalances.builder()
                            .customerId(customerId)
                            .products(products)
                            .build();
                });
    }

    private CustomerBalances filterBalancesByTypeAndDates(CustomerBalances customerBalances, String typeProduct, LocalDate startDate, LocalDate endDate) {
        List<ProductBalance> filteredProducts = customerBalances.getProducts().stream()
                .filter(product -> filterByProductType(product, typeProduct))
                .filter(product -> filterByDateRange(product, startDate, endDate))
                .collect(Collectors.toList());
        return CustomerBalances.builder()
                .customerId(customerBalances.getCustomerId())
                .products(filteredProducts)
                .build();
    }
    private boolean filterByProductType(ProductBalance product, String typeProduct) {
        if (typeProduct == null || typeProduct.isEmpty()) {
            return true;
        }
        return (product.getType() != null && product.getType().toString().equals(typeProduct)) ||
                (product.getSubType() != null && product.getSubType().toString().equals(typeProduct));
    }
    private boolean filterByDateRange(ProductBalance product, LocalDate startDate, LocalDate endDate) {
        LocalDate createdAt = product.getCreatedAt().toLocalDate();
        boolean afterStartDate = startDate == null || !createdAt.isBefore(startDate);
        boolean beforeEndDate = endDate == null || !createdAt.isAfter(endDate);
        return afterStartDate && beforeEndDate;
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

    public Mono<List<ProductMovement>> getRecentCardMovements(String customerId, String cardId, int limit) {
        return transactionClient.getTransactionsByCustomerAndProduct(customerId, cardId)
                .map(transactions -> transactions.stream()
                        .sorted(Comparator.comparing(Transaction::getTransactionDate).reversed())
                        .limit(limit)
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

    private ProductSubType mapAccountType(AccountType type) {
        switch (type) {
            case SAVINGS:
                return ProductSubType.SAVINGS;
            case CHECKING:
                return ProductSubType.CHECKING;
            case FIXED_TERM:
                return ProductSubType.FIXED_TERM;
            default:
                throw new IllegalArgumentException("Tipo de cuenta no soportado: " + type);
        }
    }
    private ProductSubType mapCreditCardType(CreditCardType type) {
        switch (type) {
            case PERSONAL_CREDIT_CARD:
                return ProductSubType.PERSONAL_CREDIT_CARD;
            case BUSINESS_CREDIT_CARD:
                return ProductSubType.BUSINESS_CREDIT_CARD;
            default:
                throw new IllegalArgumentException("Tipo de tarjeta de crédito no soportado: " + type);
        }
    }
    private ProductSubType mapCreditType(CreditType type) {
        switch (type) {
            case PERSONAL:
                return ProductSubType.PERSONAL_CREDIT;
            case BUSINESS:
                return ProductSubType.BUSINESS_CREDIT;
            default:
                throw new IllegalArgumentException("Tipo de crédito no soportado: " + type);
        }
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
            return Collections.emptyList();
        }
        Map<String, List<DailyBalance>> balancesByProduct = balances.stream()
                .collect(Collectors.groupingBy(DailyBalance::getProductId));
        return balancesByProduct.entrySet().stream()
                .map(entry -> {
                    List<DailyBalance> productBalances = entry.getValue();
                    BigDecimal totalBalance = productBalances.stream()
                            .map(d -> d.getBalance() != null ? d.getBalance() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal averageBalance = productBalances.isEmpty()
                            ? BigDecimal.ZERO
                            : totalBalance.divide(BigDecimal.valueOf(productBalances.size()), 2, RoundingMode.HALF_UP);
                    DailyBalance firstBalance = productBalances.get(0);
                    return new DailyBalanceSummary(
                            entry.getKey(),
                            firstBalance.getProductType(),
                            firstBalance.getSubType(),
                            averageBalance
                    );
                })
                .collect(Collectors.toList());
    }
    public Mono<BaseResponse<List<CategorySummary>>> fetchTransactionSummaryByDate(LocalDate startDate,
                                                                                   LocalDate endDate) {
        return transactionClient.getTransactionsByDate(startDate, endDate)
                .flatMap(transactions -> {
                    Map<ProductCategory, List<Transaction>> transactionsByCategory =
                            Arrays.stream(ProductCategory.values())
                            .collect(Collectors.toMap(category ->
                                    category, category -> new ArrayList<>()));

                    if (transactions != null && !transactions.isEmpty()) {
                        transactions.stream()
                                .filter(transaction -> transaction != null)
                                .forEach(transaction -> {
                                    ProductCategory category = transaction.getProductCategory();
                                    transactionsByCategory.get(category).add(transaction);
                                });
                    }

                    List<CategorySummary> categorySummaries =
                            transactionsByCategory.entrySet().stream()
                            .map(entry -> {
                                int quantity = entry.getValue().size();
                                double totalCommissions = entry.getValue().stream()
                                        .filter(transaction ->
                                                transaction != null && transaction.getCommissions() != null)
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