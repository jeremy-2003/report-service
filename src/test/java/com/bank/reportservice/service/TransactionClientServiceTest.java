package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.function.Function;
@ExtendWith(MockitoExtension.class)
class TransactionClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private TransactionClientService transactionClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        transactionClientService = new TransactionClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getTransactionsByCustomerAndProduct_Success() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        List<Transaction> expectedTransactions = Arrays.asList(
                createTransaction("T1", customerId, productId, new BigDecimal("100.00")),
                createTransaction("T2", customerId, productId, new BigDecimal("200.00"))
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/transactions/customer/{customerId}/product/{productId}",
                customerId, productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Transaction>> response = new BaseResponse<>();
        response.setData(expectedTransactions);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByCustomerAndProduct(customerId, productId))
                .expectNextMatches(transactions ->
                        transactions.size() == 2 &&
                                transactions.get(0).getId().equals("T1") &&
                                transactions.get(1).getId().equals("T2"))
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerAndProduct_EmptyResponse() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/transactions/customer/{customerId}/product/{productId}",
                customerId, productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Transaction>> response = new BaseResponse<>();
        response.setData(Collections.emptyList());
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByCustomerAndProduct(customerId, productId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    @Test
    void getTransactionsByCustomerAndProduct_Error() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/transactions/customer/{customerId}/product/{productId}",
                customerId, productId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Error fetching transactions")));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByCustomerAndProduct(customerId, productId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error fetching transactions"))
                .verify();
    }
    @Test
    void getTransactionsByDate_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        List<Transaction> expectedTransactions = Arrays.asList(
                createTransaction("T1", "123", "456", new BigDecimal("100.00")),
                createTransaction("T2", "123", "456", new BigDecimal("200.00"))
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Transaction>> response = new BaseResponse<>();
        response.setData(expectedTransactions);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByDate(startDate, endDate))
                .expectNextMatches(transactions ->
                        transactions.size() == 2 &&
                                transactions.get(0).getId().equals("T1") &&
                                transactions.get(1).getId().equals("T2"))
                .verifyComplete();
    }
    @Test
    void getTransactionsByDate_EmptyResponse() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Transaction>> response = new BaseResponse<>();
        response.setData(Collections.emptyList());
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByDate(startDate, endDate))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    @Test
    void getTransactionsByDate_Error() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Error fetching transactions")));
        // Act & Assert
        StepVerifier.create(transactionClientService.getTransactionsByDate(startDate, endDate))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error fetching transactions"))
                .verify();
    }
    private Transaction createTransaction(String id, String customerId, String productId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setCustomerId(customerId);
        transaction.setProductId(productId);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        return transaction;
    }
}
