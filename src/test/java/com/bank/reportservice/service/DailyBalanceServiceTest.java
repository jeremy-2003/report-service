package com.bank.reportservice.service;

import com.bank.reportservice.model.account.Account;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCard;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.customer.Customer;
import com.bank.reportservice.model.customer.CustomerType;
import com.bank.reportservice.repository.DailyBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DailyBalanceServiceTest {
    @Mock
    private AccountClientService accountService;
    @Mock
    private CreditClientService creditService;
    @Mock
    private CustomerClientService customerClientService;
    @Mock
    private DailyBalanceRepository dailyBalanceRepository;
    private DailyBalanceService dailyBalanceService;
    @BeforeEach
    void setUp() {
        dailyBalanceService = new DailyBalanceService(
                accountService,
                creditService,
                dailyBalanceRepository,
                customerClientService
        );
    }
    @Test
    void processDailyBalances_Success() {
        // Arrange
        Customer customer1 = createCustomer("1");
        Customer customer2 = createCustomer("2");
        Account account1 = createAccount("A1", "1", 1000.0, AccountType.SAVINGS);
        Account account2 = createAccount("A2", "2", 2000.0, AccountType.CHECKING);
        Credit credit1 = createCredit("C1", "1", new BigDecimal("5000.0"), CreditType.PERSONAL);
        CreditCard card1 = createCreditCard("CC1", "1", new BigDecimal("3000.0"));
        when(customerClientService.getAllCustomers())
                .thenReturn(Mono.just(Arrays.asList(customer1, customer2)));
        when(accountService.getAccountsByCustomer("1"))
                .thenReturn(Mono.just(Collections.singletonList(account1)));
        when(accountService.getAccountsByCustomer("2"))
                .thenReturn(Mono.just(Collections.singletonList(account2)));
        when(creditService.getCreditsByCustomer("1"))
                .thenReturn(Mono.just(Collections.singletonList(credit1)));
        when(creditService.getCreditsByCustomer("2"))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(creditService.getCreditCardsByCustomer("1"))
                .thenReturn(Mono.just(Collections.singletonList(card1)));
        when(creditService.getCreditCardsByCustomer("2"))
                .thenReturn(Mono.just(Collections.emptyList()));
        when(dailyBalanceRepository.save(any(DailyBalance.class)))
                .thenReturn(Mono.just(new DailyBalance()));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .verifyComplete();
        verify(dailyBalanceRepository, times(4)).save(any(DailyBalance.class));
    }
    @Test
    void processDailyBalances_WhenCustomerHasNoProducts_ShouldContinue() {
        // Arrange
        Customer customer = createCustomer("1");
        when(customerClientService.getAllCustomers())
                .thenReturn(Mono.just(Collections.singletonList(customer)));
        when(accountService.getAccountsByCustomer("1"))
                .thenReturn(Mono.empty());
        when(creditService.getCreditsByCustomer("1"))
                .thenReturn(Mono.empty());
        when(creditService.getCreditCardsByCustomer("1"))
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .verifyComplete();
        verify(dailyBalanceRepository, never()).save(any(DailyBalance.class));
    }
    @Test
    void processDailyBalances_WhenServicesError_ShouldContinue() {
        // Arrange
        Customer customer = createCustomer("1");
        when(customerClientService.getAllCustomers())
                .thenReturn(Mono.just(Collections.singletonList(customer)));
        when(accountService.getAccountsByCustomer("1"))
                .thenReturn(Mono.error(new RuntimeException("Account service error")));
        when(creditService.getCreditsByCustomer("1"))
                .thenReturn(Mono.error(new RuntimeException("Credit service error")));
        when(creditService.getCreditCardsByCustomer("1"))
                .thenReturn(Mono.error(new RuntimeException("Credit card service error")));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .verifyComplete();
        verify(dailyBalanceRepository, never()).save(any(DailyBalance.class));
    }
    @Test
    void processDailyBalances_WhenCustomerServiceFails_ShouldPropagateError() {
        // Arrange
        when(customerClientService.getAllCustomers())
                .thenReturn(Mono.error(new RuntimeException("Customer service error")));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .expectError(RuntimeException.class)
                .verify();
        verify(dailyBalanceRepository, never()).save(any(DailyBalance.class));
    }
    private Customer createCustomer(String id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName("Customer");
        customer.setCustomerType(CustomerType.PERSONAL);
        return customer;
    }
    private Account createAccount(String id, String customerId, double balance, AccountType type) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setBalance(balance);
        account.setAccountType(type);
        return account;
    }
    private Credit createCredit(String id, String customerId, BigDecimal balance, CreditType type) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId(customerId);
        credit.setRemainingBalance(balance);
        credit.setCreditType(type);
        return credit;
    }
    private CreditCard createCreditCard(String id, String customerId, BigDecimal balance) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setCustomerId(customerId);
        card.setAvailableBalance(balance);
        card.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        return card;
    }
}