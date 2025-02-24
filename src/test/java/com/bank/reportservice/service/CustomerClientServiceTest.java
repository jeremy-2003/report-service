package com.bank.reportservice.service;


import com.bank.reportservice.model.customer.Customer;
import com.bank.reportservice.model.customer.CustomerType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bank.reportservice.dto.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CustomerClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private CustomerClientService customerClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        customerClientService = new CustomerClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getAllCustomers_Success() {
        // Arrange
        List<Customer> expectedCustomers = Arrays.asList(
                createCustomer("1", "John", "Doe"),
                createCustomer("2", "Jane", "Smith")
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any())).thenReturn(responseSpec);
        BaseResponse<List<Customer>> response = new BaseResponse<>();
        response.setData(expectedCustomers);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(customerClientService.getAllCustomers())
                .expectNextMatches(customers ->
                        customers.size() == 2 &&
                                customers.get(0).getId().equals("1") &&
                                customers.get(1).getId().equals("2"))
                .verifyComplete();
    }
    @Test
    void getAllCustomers_WhenClientError_ShouldReturnError() {
        // Arrange
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        // Configuración del error 4xx
        when(responseSpec.onStatus(any(Predicate.class), any()))
                .thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Client error: 404 NOT_FOUND")));
        // Act & Assert
        StepVerifier.create(customerClientService.getAllCustomers())
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Client error: 404 NOT_FOUND"))
                .verify();
    }
    @Test
    void getAllCustomers_WhenServerError_ShouldReturnError() {
        // Arrange
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        // Configuración del error 5xx
        when(responseSpec.onStatus(any(Predicate.class), any()))
                .thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Server error: 500 INTERNAL_SERVER_ERROR")));
        // Act & Assert
        StepVerifier.create(customerClientService.getAllCustomers())
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Server error: 500 INTERNAL_SERVER_ERROR"))
                .verify();
    }
    @Test
    void getAllCustomers_WhenNullResponse_ShouldReturnEmpty() {
        // Arrange
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(Predicate.class), any())).thenReturn(responseSpec);
        BaseResponse<List<Customer>> response = new BaseResponse<>();
        response.setData(null);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(customerClientService.getAllCustomers())
                .verifyComplete();
    }
    private Customer createCustomer(String id, String firstName, String lastName) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFullName(firstName);
        customer.setCustomerType(CustomerType.PERSONAL);
        return customer;
    }
}