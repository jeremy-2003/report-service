package com.bank.reportservice.client;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.creditcard.CreditCard;
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
public class CreditClientService {
    private final WebClient webClient;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    public CreditClientService(WebClient.Builder builder,
                               @Value("${services.credit-url}") String baseUrl,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("creditService");
        log.info("Circuit breaker '{}' initialized with state: {}",
                circuitBreaker.getName(), circuitBreaker.getState());
    }

    public Mono<List<CreditCard>> getCreditCardsByCustomer(String customerId) {
        return webClient.get()
                .uri("/credit-cards/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<CreditCard>>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching credit cards for customer {}: {}",
                        customerId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch credit cards for customer {}. Reason: {}",
                            customerId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for retrieving credit cards. " +
                                    "Cannot proceed with the operation."));
                });
    }

    public Mono<List<Credit>> getCreditsByCustomer(String customerId) {
        return webClient.get()
                .uri("/credits/customer/{customerId}", customerId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    log.error("Client error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    log.error("Server error: {}", response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Credit>>>() { })
                .map(BaseResponse::getData)
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(error -> log.error("Error fetching credits for customer {}: {}",
                        customerId, error.getMessage()))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.error("FALLBACK TRIGGERED: Unable to fetch credits for customer {}. Reason: {}",
                            customerId, throwable.getMessage());
                    log.error("Exception type: {}", throwable.getClass().getName());
                    return Mono.error(new RuntimeException(
                            "Credit service is unavailable for retrieving credits. " +
                                    "Cannot proceed with the operation."));
                });
    }
}

