package com.bank.reportservice.service;

import com.bank.reportservice.client.AccountClientService;
import com.bank.reportservice.client.CreditClientService;
import com.bank.reportservice.client.CustomerClientService;
import com.bank.reportservice.client.DebitCardClientService;
import com.bank.reportservice.model.account.Account;
import com.bank.reportservice.model.account.AccountType;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCard;
import com.bank.reportservice.model.creditcard.CreditCardType;
import com.bank.reportservice.model.customer.Customer;
import com.bank.reportservice.model.debitcard.DebitCard;
import com.bank.reportservice.repository.DailyBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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
    @Mock
    private DebitCardClientService debitCardClientService;
    @InjectMocks
    private DailyBalanceService dailyBalanceService;
    private final String CUSTOMER_ID = "customer123";
    private final String ACCOUNT_ID = "account123";
    private final String CREDIT_ID = "credit123";
    private final String CREDIT_CARD_ID = "creditCard123";
    private final String DEBIT_CARD_ID = "debitCard123";
    @BeforeEach
    void setUp() {
        when(dailyBalanceRepository.save(any(DailyBalance.class)))
                .thenAnswer(invocation -> {
                    DailyBalance balance = invocation.getArgument(0);
                    balance.setId("generatedId");
                    return Mono.just(balance);
                });
    }
    @Test
    void processDailyBalances_shouldProcessAllCustomers() {
        // Arrange
        Customer customer1 = new Customer();
        customer1.setId(CUSTOMER_ID);
        Customer customer2 = new Customer();
        customer2.setId("customer456");
        List<Customer> customers = Arrays.asList(customer1, customer2);
        when(customerClientService.getAllCustomers()).thenReturn(Mono.just(customers));
        mockAccountsForCustomer(CUSTOMER_ID);
        mockCreditsForCustomer(CUSTOMER_ID);
        mockCreditCardsForCustomer(CUSTOMER_ID);
        mockDebitCardsForCustomer(CUSTOMER_ID);
        mockAccountsForCustomer("customer456");
        mockCreditsForCustomer("customer456");
        mockCreditCardsForCustomer("customer456");
        mockDebitCardsForCustomer("customer456");
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .verifyComplete();
        verify(customerClientService, times(1)).getAllCustomers();
        verify(accountService, times(2)).getAccountsByCustomer(anyString());
        verify(creditService, times(2)).getCreditsByCustomer(anyString());
        verify(creditService, times(2)).getCreditCardsByCustomer(anyString());
        verify(debitCardClientService, times(2)).getDebitCardsByCustomer(anyString());
    }
    @Test
    void saveBalancesForCustomer_shouldSaveAllProductTypes() {
        // Arrange
        mockAccountsForCustomer(CUSTOMER_ID);
        mockCreditsForCustomer(CUSTOMER_ID);
        mockCreditCardsForCustomer(CUSTOMER_ID);
        mockDebitCardsForCustomer(CUSTOMER_ID);
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveBalancesForCustomer(CUSTOMER_ID))
                .verifyComplete();
        verify(accountService, times(1)).getAccountsByCustomer(CUSTOMER_ID);
        verify(creditService, times(1)).getCreditsByCustomer(CUSTOMER_ID);
        verify(creditService, times(1)).getCreditCardsByCustomer(CUSTOMER_ID);
        verify(debitCardClientService, times(1)).getDebitCardsByCustomer(CUSTOMER_ID);
        verify(dailyBalanceRepository, times(4)).save(any(DailyBalance.class));
    }
    @Test
    void saveAccountBalances_shouldSaveAllAccounts() {
        // Arrange
        Account account1 = createAccount(ACCOUNT_ID, AccountType.SAVINGS, 1000.0);
        Account account2 = createAccount("account456", AccountType.CHECKING, 2000.0);
        List<Account> accounts = Arrays.asList(account1, account2);
        when(accountService.getAccountsByCustomer(CUSTOMER_ID)).thenReturn(Mono.just(accounts));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveAccountBalances(CUSTOMER_ID))
                .verifyComplete();
        verify(accountService, times(1)).getAccountsByCustomer(CUSTOMER_ID);
        verify(dailyBalanceRepository, times(2)).save(any(DailyBalance.class));
    }
    @Test
    void saveCreditBalances_shouldSaveAllCredits() {
        // Arrange
        Credit credit1 = createCredit(CREDIT_ID, CreditType.PERSONAL, new BigDecimal("5000.00"));
        Credit credit2 = createCredit("credit456", CreditType.BUSINESS, new BigDecimal("150000.00"));
        List<Credit> credits = Arrays.asList(credit1, credit2);
        when(creditService.getCreditsByCustomer(CUSTOMER_ID)).thenReturn(Mono.just(credits));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveCreditBalances(CUSTOMER_ID))
                .verifyComplete();
        verify(creditService, times(1)).getCreditsByCustomer(CUSTOMER_ID);
        verify(dailyBalanceRepository, times(2)).save(any(DailyBalance.class));
    }
    @Test
    void saveCreditCardBalances_shouldSaveAllCreditCards() {
        // Arrange
        CreditCard card1 = createCreditCard(CREDIT_CARD_ID,
            CreditCardType.PERSONAL_CREDIT_CARD,
            new BigDecimal("2000.00"));
        CreditCard card2 = createCreditCard("creditCard456",
            CreditCardType.BUSINESS_CREDIT_CARD,
            new BigDecimal("3000.00"));
        List<CreditCard> cards = Arrays.asList(card1, card2);
        when(creditService.getCreditCardsByCustomer(CUSTOMER_ID)).thenReturn(Mono.just(cards));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveCreditCardBalances(CUSTOMER_ID))
                .verifyComplete();
        verify(creditService, times(1)).getCreditCardsByCustomer(CUSTOMER_ID);
        verify(dailyBalanceRepository, times(2)).save(any(DailyBalance.class));
    }
    @Test
    void saveDebitCardBalances_shouldSaveAllDebitCards() {
        // Arrange
        DebitCard card = createDebitCard(DEBIT_CARD_ID, ACCOUNT_ID);
        List<DebitCard> cards = Arrays.asList(card);
        Account account = createAccount(ACCOUNT_ID, AccountType.SAVINGS, 1000.0);
        when(debitCardClientService.getDebitCardsByCustomer(CUSTOMER_ID)).thenReturn(Mono.just(cards));
        when(accountService.getAccountById(ACCOUNT_ID)).thenReturn(Mono.just(account));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveDebitCardBalances(CUSTOMER_ID))
                .verifyComplete();
        verify(debitCardClientService, times(1)).getDebitCardsByCustomer(CUSTOMER_ID);
        verify(accountService, times(1)).getAccountById(ACCOUNT_ID);
        verify(dailyBalanceRepository, times(1)).save(any(DailyBalance.class));
    }
    @Test
    void saveDebitCardBalances_shouldHandleAccountNotFound() {
        // Arrange
        DebitCard card = createDebitCard(DEBIT_CARD_ID, ACCOUNT_ID);
        List<DebitCard> cards = Arrays.asList(card);
        when(debitCardClientService.getDebitCardsByCustomer(CUSTOMER_ID)).thenReturn(Mono.just(cards));
        when(accountService.getAccountById(ACCOUNT_ID))
                .thenReturn(Mono.error(new RuntimeException("Account not found")));
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveDebitCardBalances(CUSTOMER_ID))
                .verifyComplete();
        verify(debitCardClientService, times(1)).getDebitCardsByCustomer(CUSTOMER_ID);
        verify(accountService, times(1)).getAccountById(ACCOUNT_ID);
        verify(dailyBalanceRepository, times(1)).save(any(DailyBalance.class));
    }
    @Test
    void saveDailyBalance_shouldSaveBalance() {
        // Arrange
        BigDecimal balance = new BigDecimal("1000.00");
        // Act & Assert
        StepVerifier.create(dailyBalanceService.saveDailyBalance(
                        CUSTOMER_ID, ACCOUNT_ID, "ACCOUNT", "SAVINGS", balance))
                .verifyComplete();
        verify(dailyBalanceRepository, times(1)).save(argThat(dailyBalance ->
                dailyBalance.getCustomerId().equals(CUSTOMER_ID) &&
                        dailyBalance.getProductId().equals(ACCOUNT_ID) &&
                        dailyBalance.getProductType().equals("ACCOUNT") &&
                        dailyBalance.getSubType().equals("SAVINGS") &&
                        dailyBalance.getBalance().equals(balance) &&
                        dailyBalance.getDate() != null
        ));
    }
    private void mockAccountsForCustomer(String customerId) {
        Account account = createAccount(ACCOUNT_ID, AccountType.SAVINGS, 1000.0);
        when(accountService.getAccountsByCustomer(customerId))
                .thenReturn(Mono.just(Arrays.asList(account)));
    }
    private void mockCreditsForCustomer(String customerId) {
        Credit credit = createCredit(CREDIT_ID,
            CreditType.PERSONAL, new BigDecimal("5000.00"));
        when(creditService.getCreditsByCustomer(customerId))
                .thenReturn(Mono.just(Arrays.asList(credit)));
    }
    private void mockCreditCardsForCustomer(String customerId) {
        CreditCard card = createCreditCard(CREDIT_CARD_ID,
            CreditCardType.PERSONAL_CREDIT_CARD,
            new BigDecimal("2000.00"));
        when(creditService.getCreditCardsByCustomer(customerId))
                .thenReturn(Mono.just(Arrays.asList(card)));
    }
    private void mockDebitCardsForCustomer(String customerId) {
        DebitCard card = createDebitCard(DEBIT_CARD_ID, ACCOUNT_ID);
        Account account = createAccount(ACCOUNT_ID, AccountType.SAVINGS, 1000.0);
        when(debitCardClientService.getDebitCardsByCustomer(customerId))
                .thenReturn(Mono.just(Arrays.asList(card)));
        when(accountService.getAccountById(ACCOUNT_ID))
                .thenReturn(Mono.just(account));
    }
    private Account createAccount(String id, AccountType type, double balance) {
        Account account = new Account();
        account.setId(id);
        account.setAccountType(type);
        account.setBalance(balance);
        return account;
    }
    private Credit createCredit(String id, CreditType type, BigDecimal remainingBalance) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCreditType(type);
        credit.setRemainingBalance(remainingBalance);
        return credit;
    }
    private CreditCard createCreditCard(String id, CreditCardType type, BigDecimal availableBalance) {
        CreditCard card = new CreditCard();
        card.setId(id);
        card.setCardType(type);
        card.setAvailableBalance(availableBalance);
        return card;
    }
    private DebitCard createDebitCard(String id, String accountId) {
        DebitCard card = new DebitCard();
        card.setId(id);
        card.setPrimaryAccountId(accountId);
        return card;
    }
}