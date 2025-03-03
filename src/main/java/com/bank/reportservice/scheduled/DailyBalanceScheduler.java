package com.bank.reportservice.scheduled;

import com.bank.reportservice.service.DailyBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@Slf4j
public class DailyBalanceScheduler {
    private final DailyBalanceService dailyBalanceService;

    public DailyBalanceScheduler(DailyBalanceService dailyBalanceService) {
        this.dailyBalanceService = dailyBalanceService;
    }

    @Scheduled(cron = "59 59 23 * * ?")
    public void executeDailyBalanceJob() {
        log.info("Starting the scheduled process of daily balances...");
        dailyBalanceService.processDailyBalances()
                .doOnSuccess(unused -> log.info("Daily balances process completed correctly."))
                .subscribe();
    }
}