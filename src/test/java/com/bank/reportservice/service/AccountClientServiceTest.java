package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.account.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AccountClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private AccountClientService accountClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        accountClientService = new AccountClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getAccountsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<Account> expectedAccounts = Arrays.asList(
                createAccount("1", customerId),
                createAccount("2", customerId)
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/accounts/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Account>> response = new BaseResponse<>();
        response.setData(expectedAccounts);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectNextMatches(accounts ->
                        accounts.size() == 2 &&
                                accounts.get(0).getCustomerId().equals(customerId) &&
                                accounts.get(1).getCustomerId().equals(customerId))
                .verifyComplete();
    }
    @Test
    void getAccountsByCustomer_WhenServiceError_ShouldReturnError() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/accounts/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Error fetching accounts")));
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error fetching accounts"))
                .verify();
    }
    @Test
    void getAccountsByCustomer_WhenEmptyResponse_ShouldReturnEmptyList() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/accounts/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Account>> response = new BaseResponse<>();
        response.setData(Collections.emptyList());
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(accountClientService.getAccountsByCustomer(customerId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    private Account createAccount(String id, String customerId) {
        Account account = new Account();
        account.setId(id);
        account.setCustomerId(customerId);
        account.setBalance(1000.0);
        return account;
    }
}