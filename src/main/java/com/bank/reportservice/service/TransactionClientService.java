package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.transaction.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
    public TransactionClientService(WebClient.Builder builder, @Value("${services.transaction-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = builder.baseUrl(baseUrl).build();
    }
    public Mono<List<Transaction>> getTransactionsByCustomerAndProduct(String customerId, String productId) {
        return webClient.get()
                .uri("/api/transactions/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Transaction>>>() { })
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching transactions " +
                                "for customer {} and product {}: {}",
                        customerId, productId, error.getMessage()));
    }
    public Mono<List<Transaction>> getTransactionsByDate(LocalDate startDate, LocalDate endDate) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/transactions/by-date")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Transaction>>>() { })
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching transactions by date: {}",
                        error.getMessage()));
    }

}