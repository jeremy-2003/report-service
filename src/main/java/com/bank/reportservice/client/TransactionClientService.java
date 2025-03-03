package com.bank.reportservice.client;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.transaction.Transaction;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class TransactionClientService {
    private final WebClient webClient;
    private final String baseUrl;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    public TransactionClientService(WebClient.Builder builder,
                                    @Value("${services.transaction-url}") String baseUrl,
                                    CircuitBreakerRegistry circuitBreakerRegistry) {
        this.baseUrl = baseUrl;
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("transactionService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    public Mono<List<Transaction>> getTransactionsByCustomerAndProduct(String customerId, String productId) {
        return webClient.get()
                .uri("/transactions/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Transaction>>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching transactions for customer {} and product {}: {}",
                        customerId, productId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch transactions" +
                        " for customer {} and product {}. Reason: {}",
                        customerId, productId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Transaction service is unavailable for retrieving transactions. " +
                                    "Cannot proceed with the operation."));
                });
    }

    public Mono<List<Transaction>> getTransactionsByDate(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/transactions/by-date")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Transaction>>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching transactions by date: {}", error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch transactions by date. Reason: {}",
                            throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Transaction service is unavailable for retrieving transactions by date. " +
                                    "Cannot proceed with the operation."));
                });
    }
}
