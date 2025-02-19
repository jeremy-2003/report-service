package com.bank.reportservice.config;

import com.bank.reportservice.service.AccountClientService;
import com.bank.reportservice.service.CreditClientService;
import com.bank.reportservice.service.TransactionClientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    @Bean
    public AccountClientService accountClient(
            WebClient.Builder builder,
            @Value("${services.account.url:http://localhost:8082}") String accountServiceUrl) {
        return new AccountClientService(builder, accountServiceUrl);
    }
    @Bean
    public CreditClientService creditClient(
            WebClient.Builder builder,
            @Value("${services.credit.url:http://localhost:8083}") String creditServiceUrl) {
        return new CreditClientService(builder, creditServiceUrl);
    }
    @Bean
    public TransactionClientService transactionClient(
            WebClient.Builder builder,
            @Value("${services.transaction.url:http://localhost:8084}") String transactionServiceUrl) {
        return new TransactionClientService(builder, transactionServiceUrl);
    }
}
