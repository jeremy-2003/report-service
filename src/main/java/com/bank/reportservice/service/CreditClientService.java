package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.creditcard.CreditCard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class CreditClientService {
    private final WebClient webClient;
    private final String baseUrl;
    public CreditClientService(WebClient.Builder builder, @Value("${services.credit-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = builder.baseUrl(baseUrl).build();
    }
    public Mono<List<CreditCard>> getCreditCardsByCustomer(String customerId) {
        return webClient.get()
                .uri("/api/credit-cards/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<CreditCard>>>() {})
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching credit cards for customer {}: {}",
                        customerId, error.getMessage()));
    }
    public Mono<List<Credit>> getCreditsByCustomer(String customerId) {
        return webClient.get()
                .uri("/api/credits/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Credit>>>() {})
                .map(BaseResponse::getData)
                .doOnError(error -> log.error("Error fetching credits for customer {}: {}",
                        customerId, error.getMessage()));
    }
}
