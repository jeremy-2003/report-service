package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.debitcard.DebitCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class DebitCardClientService {
    private final WebClient webClient;
    @Autowired
    public DebitCardClientService(@Value("${services.account-url}") String accountServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }
    public Mono<DebitCard> getDebitCardById(String cardId) {
        return webClient.get()
                .uri("/debit-cards/{cardId}", cardId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<DebitCard>>() { })
                .flatMap(response -> {
                    if (response.getStatus() == 200 && response.getData() != null) {
                        return Mono.just(response.getData());
                    } else {
                        return Mono.error(new RuntimeException("Debit card not found or error: " + response.getMessage()));
                    }
                });
    }
    public Mono<List<DebitCard>> getDebitCardsByCustomer(String customerId) {
        return webClient.get()
                .uri("/debit-cards/customer/{customerId}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<DebitCard>>>() { })
                .map(BaseResponse::getData)
                .doOnError(e -> log.error("Error retrieving debit cards for customer {}: {}", customerId, e.getMessage()));
    }
}
