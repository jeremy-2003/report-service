package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class TransactionClientService {
    private final WebClient webClient;
    public TransactionClientService(WebClient.Builder builder, String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }
    public Mono<List<Transaction>> getTransactionsByCustomerAndProduct(String customerId, String productId) {
        return webClient.get()
                .uri("/api/transactions/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Transaction>>>() {})
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching transactions for customer {} and product {}: {}",
                        customerId, productId, error.getMessage()));
    }
}