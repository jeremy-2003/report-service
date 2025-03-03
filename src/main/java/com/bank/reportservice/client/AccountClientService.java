package com.bank.reportservice.client;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.account.Account;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
@Service
@Slf4j
public class AccountClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    public AccountClientService(WebClient.Builder builder,
                                @Value("${services.account-url}") String baseUrl,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("accountService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    public Mono<List<Account>> getAccountsByCustomer(String customerId) {
        return webClient.get()
                .uri("/accounts/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Account>>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching accounts for customer {}: {}",
                        customerId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch accounts for customer {}. Reason: {}",
                            customerId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Account service is unavailable for retrieving accounts. " +
                                    "Cannot proceed with the operation."));
                });
    }

    public Mono<Account> getAccountById(String accountId) {
        return webClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching account with ID {}: {}", accountId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch account with ID {}. Reason: {}",
                            accountId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Account service is unavailable for retrieving account. " +
                                    "Cannot proceed with the operation."));
                });
    }
}

