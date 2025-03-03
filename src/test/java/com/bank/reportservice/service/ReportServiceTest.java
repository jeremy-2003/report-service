package com.bank.reportservice.service;

import com.bank.reportservice.client.AccountClientService;
import com.bank.reportservice.client.CreditClientService;
import com.bank.reportservice.client.DebitCardClientService;
import com.bank.reportservice.client.TransactionClientService;
import com.bank.reportservice.dto.CategorySummary;
import com.bank.reportservice.dto.DailyBalanceSummary;
import com.bank.reportservice.dto.ProductBalance;
import com.bank.reportservice.dto.ProductMovement;
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
import com.bank.reportservice.model.transaction.TransactionType;
import com.bank.reportservice.repository.DailyBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {
    @Mock
    private AccountClientService accountClient;
    @Mock
    private CreditClientService creditClient;
    @Mock
    private TransactionClientService transactionClient;
    @Mock
    private DailyBalanceRepository dailyBalanceRepository;
    @Mock
    private DebitCardClientService debitCardClientService;
    @InjectMocks
    private ReportService reportService;
    private String customerId;
    private Account account;
    private CreditCard creditCard;
    private Credit credit;
    private DebitCard debitCard;
    private Transaction transaction;
    private DailyBalance dailyBalance;
    @BeforeEach
    void setUp() {
        customerId = "customer123";
        account = new Account();
        account.setId("account123");
        account.setCustomerId(customerId);
        account.setAccountType(AccountType.SAVINGS);
        account.setBalance(1000.0);
        account.setCreatedAt(LocalDateTime.now().minusDays(30));
        creditCard = new CreditCard();
        creditCard.setId("creditCard123");
        creditCard.setCustomerId(customerId);
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        creditCard.setCreditLimit(new BigDecimal("5000.00"));
        creditCard.setAvailableBalance(new BigDecimal("3000.00"));
        creditCard.setStatus("ACTIVE");
        creditCard.setCreatedAt(LocalDateTime.now().minusDays(60));
        credit = new Credit();
        credit.setId("credit123");
        credit.setCustomerId(customerId);
        credit.setCreditType(CreditType.PERSONAL);
        credit.setAmount(new BigDecimal("10000.00"));
        credit.setRemainingBalance(new BigDecimal("8000.00"));
        credit.setCreatedAt(LocalDateTime.now().minusDays(90));
        debitCard = new DebitCard();
        debitCard.setId("debitCard123");
        debitCard.setCustomerId(customerId);
        debitCard.setPrimaryAccountId("account123");
        debitCard.setStatus("ACTIVE");
        debitCard.setCreatedAt(LocalDateTime.now().minusDays(15));
        transaction = new Transaction();
        transaction.setId("transaction123");
        transaction.setCustomerId(customerId);
        transaction.setProductId("account123");
        transaction.setTransactionDate(LocalDateTime.now().minusDays(5));
        transaction.setAmount(new BigDecimal("500.00"));
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setProductCategory(ProductCategory.ACCOUNT);
        transaction.setProductSubType(ProductSubType.SAVINGS);
        transaction.setCommissions(new BigDecimal("5.00"));
        dailyBalance = new DailyBalance();
        dailyBalance.setId("dailyBalance123");
        dailyBalance.setCustomerId(customerId);
        dailyBalance.setProductId("account123");
        dailyBalance.setProductType(ProductCategory.ACCOUNT.toString());
        dailyBalance.setSubType(ProductSubType.SAVINGS.toString());
        dailyBalance.setBalance(new BigDecimal("1000.00"));
        dailyBalance.setDate(LocalDateTime.now().minusDays(1));
    }
    @Test
    void getCustomerBalances_Success() {
        // Arrange
        List<Account> accounts = Collections.singletonList(account);
        List<CreditCard> creditCards = Collections.singletonList(creditCard);
        List<Credit> credits = Collections.singletonList(credit);
        List<DebitCard> debitCards = Collections.singletonList(debitCard);
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(accounts));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(creditCards));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(credits));
        when(debitCardClientService.getDebitCardsByCustomer(customerId)).thenReturn(Mono.just(debitCards));
        when(accountClient.getAccountById(anyString())).thenReturn(Mono.just(account));
        // Act & Assert
        StepVerifier.create(reportService.getCustomerBalances(customerId))
                .assertNext(customerBalances -> {
                    assertEquals(customerId, customerBalances.getCustomerId());
                    assertEquals(4, customerBalances.getProducts().size());
                    ProductBalance accountBalance = findProductById(customerBalances.getProducts(), "account123");
                    assertNotNull(accountBalance);
                    assertEquals(ProductCategory.ACCOUNT, accountBalance.getType());
                    assertEquals(ProductSubType.SAVINGS, accountBalance.getSubType());
                    assertEquals(new BigDecimal("1000.0"), accountBalance.getAvailableBalance());
                    ProductBalance creditCardBalance = findProductById(customerBalances.getProducts(), "creditCard123");
                    assertNotNull(creditCardBalance);
                    assertEquals(ProductCategory.CREDIT_CARD, creditCardBalance.getType());
                    assertEquals(ProductSubType.PERSONAL_CREDIT_CARD, creditCardBalance.getSubType());
                    assertEquals(new BigDecimal("3000.00"), creditCardBalance.getAvailableBalance());
                    ProductBalance creditBalance = findProductById(customerBalances.getProducts(), "credit123");
                    assertNotNull(creditBalance);
                    assertEquals(ProductCategory.CREDIT, creditBalance.getType());
                    assertEquals(ProductSubType.PERSONAL_CREDIT, creditBalance.getSubType());
                    assertEquals(new BigDecimal("8000.00"), creditBalance.getAvailableBalance());
                    ProductBalance debitCardBalance = findProductById(customerBalances.getProducts(), "debitCard123");
                    assertNotNull(debitCardBalance);
                    assertEquals(ProductCategory.DEBIT_CARD, debitCardBalance.getType());
                    assertEquals(new BigDecimal("1000.0"), debitCardBalance.getAvailableBalance());
                })
                .verifyComplete();
        verify(accountClient).getAccountsByCustomer(customerId);
        verify(creditClient).getCreditCardsByCustomer(customerId);
        verify(creditClient).getCreditsByCustomer(customerId);
        verify(debitCardClientService).getDebitCardsByCustomer(customerId);
        verify(accountClient).getAccountById(debitCard.getPrimaryAccountId());
    }
    @Test
    void getResumeByProductAndUserAndDates_FilterByTypeAndDates_Success() {
        // Arrange
        List<Account> accounts = Collections.singletonList(account);
        List<CreditCard> creditCards = Collections.singletonList(creditCard);
        List<Credit> credits = Collections.singletonList(credit);
        List<DebitCard> debitCards = Collections.singletonList(debitCard);
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(accounts));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(creditCards));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(credits));
        when(debitCardClientService.getDebitCardsByCustomer(customerId)).thenReturn(Mono.just(debitCards));
        when(accountClient.getAccountById(anyString())).thenReturn(Mono.just(account));
        String typeProduct = "ACCOUNT";
        LocalDate startDate = LocalDate.now().minusDays(60);
        LocalDate endDate = LocalDate.now();
        // Act & Assert
        StepVerifier.create(reportService.getResumeByProductAndUserAndDates(typeProduct,
            customerId, startDate, endDate))
                .assertNext(customerBalances -> {
                    assertEquals(customerId, customerBalances.getCustomerId());
                    assertEquals(1, customerBalances.getProducts().size());
                    ProductBalance accountBalance = customerBalances.getProducts().get(0);
                    assertEquals(ProductCategory.ACCOUNT, accountBalance.getType());
                    assertEquals(ProductSubType.SAVINGS, accountBalance.getSubType());
                    assertEquals("account123", accountBalance.getProductId());
                })
                .verifyComplete();
        verify(accountClient).getAccountsByCustomer(customerId);
        verify(creditClient).getCreditCardsByCustomer(customerId);
        verify(creditClient).getCreditsByCustomer(customerId);
        verify(debitCardClientService).getDebitCardsByCustomer(customerId);
    }
    @Test
    void getProductMovements_Success() {
        // Arrange
        List<Transaction> transactions = Arrays.asList(transaction);
        when(transactionClient.getTransactionsByCustomerAndProduct(customerId, "account123"))
                .thenReturn(Mono.just(transactions));
        // Act & Assert
        StepVerifier.create(reportService.getProductMovements(customerId, "account123"))
                .assertNext(movements -> {
                    assertEquals(1, movements.size());
                    ProductMovement movement = movements.get(0);
                    assertEquals("transaction123", movement.getTransactionId());
                    assertEquals(transaction.getTransactionDate(), movement.getDate());
                    assertEquals(transaction.getAmount(), movement.getAmount());
                    assertEquals(transaction.getTransactionType(), movement.getType());
                    assertEquals(transaction.getProductCategory(), movement.getProductCategory());
                    assertEquals(transaction.getProductSubType(), movement.getProductSubType());
                })
                .verifyComplete();
        verify(transactionClient).getTransactionsByCustomerAndProduct(customerId, "account123");
    }
    @Test
    void getRecentCardMovements_Success() {
        // Arrange
        Transaction transaction1 = new Transaction();
        transaction1.setId("transaction1");
        transaction1.setTransactionDate(LocalDateTime.now().minusDays(1));
        transaction1.setAmount(new BigDecimal("100.00"));
        Transaction transaction2 = new Transaction();
        transaction2.setId("transaction2");
        transaction2.setTransactionDate(LocalDateTime.now().minusDays(2));
        transaction2.setAmount(new BigDecimal("200.00"));
        Transaction transaction3 = new Transaction();
        transaction3.setId("transaction3");
        transaction3.setTransactionDate(LocalDateTime.now().minusDays(3));
        transaction3.setAmount(new BigDecimal("300.00"));
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2, transaction3);
        when(transactionClient.getTransactionsByCustomerAndProduct(customerId, "creditCard123"))
                .thenReturn(Mono.just(transactions));
        // Act & Assert
        StepVerifier.create(reportService.getRecentCardMovements(customerId, "creditCard123", 2))
                .assertNext(movements -> {
                    assertEquals(2, movements.size());
                    assertEquals("transaction1", movements.get(0).getTransactionId());
                    assertEquals("transaction2", movements.get(1).getTransactionId());
                })
                .verifyComplete();
        verify(transactionClient).getTransactionsByCustomerAndProduct(customerId, "creditCard123");
    }
    @Test
    void getMonthlyBalanceSummary_Success() {
        // Arrange
        List<DailyBalance> balances = Arrays.asList(
                dailyBalance,
                createDailyBalanceWithAmount("account123", "1100.00"),
                createDailyBalanceWithAmount("account123", "1200.00"),
                createDailyBalanceWithAmount("creditCard123", "3000.00"),
                createDailyBalanceWithAmount("creditCard123", "2800.00")
        );
        when(dailyBalanceRepository.findByCustomerIdAndDateBetween(
                eq(customerId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.fromIterable(balances));
        // Act & Assert
        StepVerifier.create(reportService.getMonthlyBalanceSummary(customerId))
                .assertNext(summaries -> {
                    assertEquals(2, summaries.size());
                    DailyBalanceSummary accountSummary = findSummaryById(summaries, "account123");
                    assertNotNull(accountSummary);
                    assertEquals(ProductCategory.ACCOUNT.toString(), accountSummary.getProductType());
                    assertEquals(ProductSubType.SAVINGS.toString(), accountSummary.getSubType());
                    assertEquals(new BigDecimal("1100.00"), accountSummary.getAverageBalance());
                    DailyBalanceSummary creditCardSummary = findSummaryById(summaries, "creditCard123");
                    assertNotNull(creditCardSummary);
                    assertEquals(ProductCategory.ACCOUNT.toString(), creditCardSummary.getProductType());
                    assertEquals(ProductSubType.SAVINGS.toString(), creditCardSummary.getSubType());
                    assertEquals(new BigDecimal("2900.00"), creditCardSummary.getAverageBalance());
                })
                .verifyComplete();
        verify(dailyBalanceRepository).findByCustomerIdAndDateBetween(
                eq(customerId), any(LocalDateTime.class), any(LocalDateTime.class));
    }
    @Test
    void fetchTransactionSummaryByDate_Success() {
        // Arrange
        List<Transaction> transactions = new ArrayList<>();
        Transaction accountTx1 = new Transaction();
        accountTx1.setProductCategory(ProductCategory.ACCOUNT);
        accountTx1.setCommissions(new BigDecimal("5.00"));
        transactions.add(accountTx1);
        Transaction accountTx2 = new Transaction();
        accountTx2.setProductCategory(ProductCategory.ACCOUNT);
        accountTx2.setCommissions(new BigDecimal("10.00"));
        transactions.add(accountTx2);
        Transaction creditCardTx = new Transaction();
        creditCardTx.setProductCategory(ProductCategory.CREDIT_CARD);
        creditCardTx.setCommissions(new BigDecimal("15.00"));
        transactions.add(creditCardTx);
        when(transactionClient.getTransactionsByDate(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Mono.just(transactions));
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        // Act & Assert
        StepVerifier.create(reportService.fetchTransactionSummaryByDate(startDate, endDate))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK.value(), response.getStatus());
                    assertEquals("Transactions retrieved successfully", response.getMessage());
                    List<CategorySummary> summaries = response.getData();
                    assertEquals(4, summaries.size());
                    CategorySummary accountSummary = findCategorySummary(summaries, "ACCOUNT");
                    assertNotNull(accountSummary);
                    assertEquals(2, accountSummary.getQuantity());
                    assertEquals(15.0, accountSummary.getCommissions(), 0.001);
                    CategorySummary creditCardSummary = findCategorySummary(summaries, "CREDIT_CARD");
                    assertNotNull(creditCardSummary);
                    assertEquals(1, creditCardSummary.getQuantity());
                    assertEquals(15.0, creditCardSummary.getCommissions(), 0.001);
                })
                .verifyComplete();
        verify(transactionClient).getTransactionsByDate(startDate, endDate);
    }
    @Test
    void getCustomerBalances_EmptyProducts() {
        // Arrange
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        when(debitCardClientService.getDebitCardsByCustomer(customerId)).thenReturn(Mono.just(Collections.emptyList()));
        // Act & Assert
        StepVerifier.create(reportService.getCustomerBalances(customerId))
                .assertNext(customerBalances -> {
                    assertEquals(customerId, customerBalances.getCustomerId());
                    assertEquals(0, customerBalances.getProducts().size());
                })
                .verifyComplete();
    }
    @Test
    void getMonthlyBalanceSummary_EmptyBalances() {
        // Arrange
        when(dailyBalanceRepository.findByCustomerIdAndDateBetween(
                eq(customerId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        // Act & Assert
        StepVerifier.create(reportService.getMonthlyBalanceSummary(customerId))
                .assertNext(summaries -> {
                    assertTrue(summaries.isEmpty());
                })
                .verifyComplete();
    }
    @Test
    void getProductMovements_EmptyTransactions() {
        // Arrange
        when(transactionClient.getTransactionsByCustomerAndProduct(customerId, "account123"))
                .thenReturn(Mono.just(Collections.emptyList()));
        // Act & Assert
        StepVerifier.create(reportService.getProductMovements(customerId, "account123"))
                .assertNext(movements -> {
                    assertTrue(movements.isEmpty());
                })
                .verifyComplete();
    }
    @Test
    void debitCardWithoutValidAccount_ShouldBeSkipped() {
        // Arrange
        List<Account> accounts = Collections.singletonList(account);
        List<CreditCard> creditCards = Collections.singletonList(creditCard);
        List<Credit> credits = Collections.singletonList(credit);
        List<DebitCard> debitCards = Collections.singletonList(debitCard);
        when(accountClient.getAccountsByCustomer(customerId)).thenReturn(Mono.just(accounts));
        when(creditClient.getCreditCardsByCustomer(customerId)).thenReturn(Mono.just(creditCards));
        when(creditClient.getCreditsByCustomer(customerId)).thenReturn(Mono.just(credits));
        when(debitCardClientService.getDebitCardsByCustomer(customerId))
            .thenReturn(Mono.just(debitCards));
        when(accountClient.getAccountById(anyString()))
            .thenReturn(Mono.error(new RuntimeException("Account not found")));
        // Act & Assert
        StepVerifier.create(reportService.getCustomerBalances(customerId))
                .assertNext(customerBalances -> {
                    assertEquals(customerId, customerBalances.getCustomerId());
                    assertEquals(3, customerBalances.getProducts().size());
                    ProductBalance debitCardBalance = findProductById(customerBalances.getProducts(), "debitCard123");
                    assertNull(debitCardBalance);
                })
                .verifyComplete();
    }
    private ProductBalance findProductById(List<ProductBalance> products, String productId) {
        return products.stream()
                .filter(p -> p.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }
    private DailyBalanceSummary findSummaryById(List<DailyBalanceSummary> summaries, String productId) {
        return summaries.stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }
    private CategorySummary findCategorySummary(List<CategorySummary> summaries, String category) {
        return summaries.stream()
                .filter(s -> s.getCategory().equals(category))
                .findFirst()
                .orElse(null);
    }
    private DailyBalance createDailyBalanceWithAmount(String productId, String amount) {
        DailyBalance balance = new DailyBalance();
        balance.setId("dailyBalance" + productId + amount);
        balance.setCustomerId(customerId);
        balance.setProductId(productId);
        balance.setProductType(ProductCategory.ACCOUNT.toString());
        balance.setSubType(ProductSubType.SAVINGS.toString());
        balance.setBalance(new BigDecimal(amount));
        balance.setDate(LocalDateTime.now().minusDays(1));
        return balance;
    }
}