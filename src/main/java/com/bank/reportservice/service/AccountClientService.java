package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.account.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AccountClientService {
    private final WebClient webClient;
    public AccountClientService(WebClient.Builder builder, String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }
    public Mono<List<Account>> getAccountsByCustomer(String customerId) {
        return webClient.get()
                .uri("/api/accounts/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Account>>>() {})
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching accounts for customer {}: {}",
                        customerId, error.getMessage()));
    }
}
