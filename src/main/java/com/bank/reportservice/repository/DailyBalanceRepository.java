package com.bank.reportservice.repository;

import com.bank.reportservice.model.balance.DailyBalance;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface DailyBalanceRepository extends ReactiveMongoRepository <DailyBalance, String> {
    Flux<DailyBalance> findByCustomerIdAndDateBetween(String customerId, LocalDateTime first, LocalDateTime last);
}
