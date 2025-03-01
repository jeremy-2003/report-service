package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
@Service
@Slf4j
public class AccountClientService {
    private final WebClient webClient;
    private final String baseUrl;
    public AccountClientService(WebClient.Builder builder, @Value("${services.account-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = builder.baseUrl(baseUrl).build();
    }
    public Mono<List<Account>> getAccountsByCustomer(String customerId) {
        return webClient.get()
                .uri("/accounts/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Account>>>() { })
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching accounts for customer {}: {}",
                        customerId, error.getMessage()));
    }
    public Mono<Account> getAccountById(String accountId) {
        return webClient.get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Account>>() { })
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching account with ID {}: {}", accountId, error.getMessage()));
    }
}
