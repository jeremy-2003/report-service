package com.bank.reportservice.service;
import com.bank.reportservice.model.balance.DailyBalance;
import com.bank.reportservice.repository.DailyBalanceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class DailyBalanceService {
    private final AccountClientService accountService;
    private final CreditClientService creditService;
    private final CustomerClientService customerClientService;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final DebitCardClientService debitCardClientService;
    public DailyBalanceService(AccountClientService accountService,
                               CreditClientService creditService,
                               DailyBalanceRepository dailyBalanceRepository,
                               CustomerClientService customerClientService,
                               DebitCardClientService debitCardClientService) {
        this.accountService = accountService;
        this.creditService = creditService;
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.customerClientService = customerClientService;
        this.debitCardClientService = debitCardClientService;
    }

    public Mono<Void> processDailyBalances() {
        log.info("Starting calculation of daily balances...");
        return customerClientService.getAllCustomers()
                .flatMapMany(Flux::fromIterable)
                .flatMap(customer -> saveBalancesForCustomer(customer.getId()))
                .then();
    }

    private Flux<Void> saveBalancesForCustomer(String customerId) {
        return Flux.merge(
                saveAccountBalances(customerId)
                        .onErrorResume(e -> {
                            log.warn("No accounts found for customer {}, continuing without accounts.", customerId);
                            return Flux.empty();
                        }),
                saveCreditBalances(customerId)
                        .onErrorResume(e -> {
                            log.warn("No credits found for customer {}, continuing " +
                                    "without credits.", customerId);
                            return Flux.empty();
                        }),
                saveCreditCardBalances(customerId)
                        .onErrorResume(e -> {
                            log.warn("No credit cards found for customer {}, " +
                                    "continuing without credit cards.", customerId);
                            return Flux.empty();
                        }),
                saveDebitCardBalances(customerId)
                        .onErrorResume(e -> {
                            log.warn("No debit cards found for customer {}, " +
                                    "continuing without debit cards.", customerId);
                            return Flux.empty();
                        })
        ).thenMany(Flux.empty());
    }

    private Flux<Void> saveAccountBalances(String customerId) {
        return accountService.getAccountsByCustomer(customerId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(account -> saveDailyBalance(customerId, account.getId(), "ACCOUNT",
                        account.getAccountType().name(), BigDecimal.valueOf(account.getBalance())));
    }

    private Flux<Void> saveCreditBalances(String customerId) {
        return creditService.getCreditsByCustomer(customerId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(credit -> saveDailyBalance(customerId, credit.getId(), "CREDIT",
                        credit.getCreditType().name(), credit.getRemainingBalance()));
    }

    private Flux<Void> saveCreditCardBalances(String customerId) {
        return creditService.getCreditCardsByCustomer(customerId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(card -> saveDailyBalance(customerId, card.getId(), "CREDIT_CARD",
                        card.getCardType().name(), card.getAvailableBalance()));
    }
    private Flux<Void> saveDebitCardBalances(String customerId) {
        return debitCardClientService.getDebitCardsByCustomer(customerId)
                .flatMapMany(Flux::fromIterable)
                .flatMap(debitCard -> {
                    String primaryAccountId = debitCard.getPrimaryAccountId();
                    return accountService.getAccountById(primaryAccountId)
                            .flatMap(account -> {
                                BigDecimal accountBalance = BigDecimal.valueOf(account.getBalance());
                                return saveDailyBalance(
                                        customerId,
                                        debitCard.getId(),
                                        "DEBIT_CARD",
                                        null,
                                        accountBalance
                                );
                            })
                            .onErrorResume(e -> {
                                log.error("Error obtaining balance for debit card {}, account {}: {}",
                                        debitCard.getId(), primaryAccountId, e.getMessage());
                                return saveDailyBalance(
                                        customerId,
                                        debitCard.getId(),
                                        "DEBIT_CARD",
                                        "DEBIT",
                                        BigDecimal.ZERO
                                );
                            });
                });
    }
    private Mono<Void> saveDailyBalance(String customerId, String productId, String productType,
                                        String subType, BigDecimal balance) {
        DailyBalance dailyBalance = new DailyBalance();
        dailyBalance.setCustomerId(customerId);
        dailyBalance.setProductId(productId);
        dailyBalance.setProductType(productType);
        dailyBalance.setSubType(subType);
        dailyBalance.setBalance(balance);
        dailyBalance.setDate(LocalDateTime.now());
        return dailyBalanceRepository.save(dailyBalance)
                .doOnSuccess(db -> log.info("Daily balance saved for {} - {}", productType, productId))
                .then();
    }
}
