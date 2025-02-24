package com.bank.reportservice.service;

import com.bank.reportservice.dto.CategorySummary;
import com.bank.reportservice.dto.DailyBalanceSummary;
import com.bank.reportservice.model.account.Account;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCard;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import com.bank.reportservice.model.transaction.Transaction;
import com.bank.reportservice.repository.DailyBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    @Mock
    private AccountClientService accountClient;
    @Mock
    private CreditClientService creditClient;
    @Mock
    private TransactionClientService transactionClient;
    @Mock
    private DailyBalanceRepository dailyBalanceRepository;
    private ReportService reportService;
    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                accountClient,
                creditClient,
                transactionClient,
                dailyBalanceRepository
        );
    }
    @Test
    void getCustomerBalances_Success() {
        // Arrange
        String customerId = "123";
        List<Account> accounts = Arrays.asList(
                createAccount("A1", customerId, 1000.0, AccountType.SAVINGS),
                createAccount("A2", customerId, 2000.0, AccountType.CHECKING)
        );
        List<CreditCard> creditCards = Arrays.asList(
                createCreditCard("CC1", customerId, new BigDecimal("5000.0"), CreditCardType.PERSONAL_CREDIT_CARD)
        );
        List<Credit> credits = Arrays.asList(
                createCredit("C1", customerId, new BigDecimal("10000.0"), CreditType.PERSONAL)
        );
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(accounts));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(creditCards));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(credits));
        // Act & Assert
        StepVerifier.create(reportService.getCustomerBalances(customerId))
                .expectNextMatches(balances -> {
                    assertEquals(customerId, balances.getCustomerId());
                    assertEquals(4, balances.getProducts().size());
                    assertTrue(balances.getProducts().stream()
                            .anyMatch(p -> p.getType() == ProductCategory.ACCOUNT &&
                                    p.getSubType() == ProductSubType.SAVINGS));
                    assertTrue(balances.getProducts().stream()
                            .anyMatch(p -> p.getType() == ProductCategory.CREDIT_CARD));
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void getProductMovements_Success() {
        // Arrange
        String customerId = "123";
        String productId = "A1";
        List<Transaction> transactions = Arrays.asList(
                createTransaction("T1", customerId, productId, new BigDecimal("100.0")),
                createTransaction("T2", customerId, productId, new BigDecimal("-50.0"))
        );
        when(transactionClient.getTransactionsByCustomerAndProduct(customerId, productId))
                .thenReturn(Mono.just(transactions));
        // Act & Assert
        StepVerifier.create(reportService.getProductMovements(customerId, productId))
                .expectNextMatches(movements -> {
                    assertEquals(2, movements.size());
                    assertEquals("T1", movements.get(0).getTransactionId());
                    assertEquals("T2", movements.get(1).getTransactionId());
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void getMonthlyBalanceSummary_Success() {
        // Arrange
        String customerId = "123";
        List<DailyBalance> balances = Arrays.asList(
                createDailyBalance(customerId, "A1", "ACCOUNT", "SAVINGS", new BigDecimal("1000.0")),
                createDailyBalance(customerId, "A1", "ACCOUNT", "SAVINGS", new BigDecimal("1200.0"))
        );
        when(dailyBalanceRepository.findByCustomerIdAndDateBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.fromIterable(balances));
        // Act & Assert
        StepVerifier.create(reportService.getMonthlyBalanceSummary(customerId))
                .expectNextMatches(summaries -> {
                    assertEquals(1, summaries.size());
                    DailyBalanceSummary summary = summaries.get(0);
                    assertEquals("A1", summary.getProductId());
                    assertEquals("ACCOUNT", summary.getProductType());
                    assertEquals("SAVINGS", summary.getSubType());
                    assertEquals(0, new BigDecimal("1100.00").compareTo(summary.getAverageBalance()));
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void fetchTransactionSummaryByDate_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Transaction> transactions = Arrays.asList(
                createTransaction("T1", "123", "A1", new BigDecimal("100.0")),
                createTransaction("T2", "123", "CC1", new BigDecimal("200.0"))
        );
        when(transactionClient.getTransactionsByDate(startDate, endDate))
                .thenReturn(Mono.just(transactions));
        // Act & Assert
        StepVerifier.create(reportService.fetchTransactionSummaryByDate(startDate, endDate))
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatus());
                    List<CategorySummary> summaries = response.getData();
                    assertTrue(summaries.stream()
                            .anyMatch(s -> s.getCategory().equals(ProductCategory.ACCOUNT.toString())));
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void getCustomerBalances_WhenNoProducts_ShouldReturnEmptyList() {
        // Arrange
        String customerId = "123";
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        // Act & Assert
        StepVerifier.create(reportService.getCustomerBalances(customerId))
                .expectNextMatches(balances -> {
                    assertEquals(customerId, balances.getCustomerId());
                    assertTrue(balances.getProducts().isEmpty());
                    return true;
                })
                .verifyComplete();
    }
    @Test
    void getMonthlyBalanceSummary_WhenNoBalances_ShouldReturnEmptyList() {
        // Arrange
        String customerId = "123";
        when(dailyBalanceRepository.findByCustomerIdAndDateBetween(
                anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(reportService.getMonthlyBalanceSummary(customerId))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
    @Test
    void fetchTransactionSummaryByDate_WhenNoTransactions_ShouldReturnEmptySummaries() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        when(transactionClient.getTransactionsByDate(startDate, endDate))
                .thenReturn(Mono.just(Collections.emptyList()));
        // Act & Assert
        StepVerifier.create(reportService.fetchTransactionSummaryByDate(startDate, endDate))
                .expectNextMatches(response -> {
                    assertEquals(200, response.getStatus());
                    assertTrue(response.getData().stream()
                            .allMatch(summary -> summary.getQuantity() == 0));
                    return true;
                })
                .verifyComplete();
    }
    // Helper methods to create test objects
    private Account createAccount(String id, String customerId, double balance, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setBalance(balance);
        account.setAccountType(type);
        return account;
    }
    private CreditCard createCreditCard(String id, String customerId, BigDecimal balance, CreditCardType type) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setCustomerId(customerId);
        card.setAvailableBalance(balance);
        card.setCardType(type);
        return card;
    }
    private Credit createCredit(String id, String customerId, BigDecimal balance, CreditType type) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId(customerId);
        credit.setRemainingBalance(balance);
        credit.setCreditType(type);
        return credit;
    }
    private Transaction createTransaction(String id, String customerId, String productId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setCustomerId(customerId);
        transaction.setProductId(productId);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setProductCategory(ProductCategory.ACCOUNT);
        transaction.setCommissions(BigDecimal.TEN);
        return transaction;
    }
    private DailyBalance createDailyBalance(String customerId, String productId,
                                            String productType, String subType, BigDecimal balance) {
        DailyBalance dailyBalance = new DailyBalance();
        dailyBalance.setCustomerId(customerId);
        dailyBalance.setProductId(productId);
        dailyBalance.setProductType(productType);
        dailyBalance.setSubType(subType);
        dailyBalance.setBalance(balance);
        dailyBalance.setDate(LocalDateTime.now());
        return dailyBalance;
    }
}