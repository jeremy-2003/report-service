package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.customer.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class CustomerClientService {
    private final WebClient webClient;
    private final String customerServiceUrl;
    public CustomerClientService(WebClient.Builder webClientBuilder,
                                 @Value("${services.customer-url}") String customerServiceUrl) {
        this.customerServiceUrl = customerServiceUrl;
        this.webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
    }
    public Mono<List<Customer>> getAllCustomers() {
        log.info("Sending request to Customer Service API: {}", customerServiceUrl);
        return webClient.get()
                .uri("/")
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                        Mono.error(new RuntimeException("Client error: " + response.statusCode()))
                )
                .onStatus(HttpStatus::is5xxServerError, response ->
                        Mono.error(new RuntimeException("Server error: " + response.statusCode()))
                )
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<Customer>>>() {})
                .flatMap(response -> {
                    if(response.getData() != null){
                        return Mono.just(response.getData());
                    } else {
                        return Mono.empty();
                    }
                })
                .doOnNext(result -> log.info("Customer API response: {}", result))
                .doOnError(e -> log.error("Error while fetching all customers: {}", e.getMessage()))
                .doOnTerminate(() -> log.info("Request to get all customers from Customer API completed"));
    }
}