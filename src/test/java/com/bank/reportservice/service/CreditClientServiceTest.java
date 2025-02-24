package com.bank.reportservice.service;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.model.credit.Credit;
import com.bank.reportservice.model.credit.CreditType;
import com.bank.reportservice.model.creditcard.CreditCard;
import com.bank.reportservice.model.creditcard.CreditCardType;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class CreditClientServiceTest {
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.Builder webClientBuilder;
    private CreditClientService creditClientService;
    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        creditClientService = new CreditClientService(webClientBuilder, "http://localhost:8080");
    }
    @Test
    void getCreditCardsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<CreditCard> expectedCreditCards = Arrays.asList(
                createCreditCard("1", customerId),
                createCreditCard("2", customerId)
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<CreditCard>> response = new BaseResponse<>();
        response.setData(expectedCreditCards);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectNextMatches(creditCards ->
                        creditCards.size() == 2 &&
                                creditCards.get(0).getCustomerId().equals(customerId) &&
                                creditCards.get(1).getCustomerId().equals(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_WhenEmptyResponse_ShouldReturnEmptyList() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<CreditCard>> response = new BaseResponse<>();
        response.setData(Collections.emptyList());
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    @Test
    void getCreditCardsByCustomer_WhenError_ShouldPropagateError() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credit-cards/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Error fetching credit cards")));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditCardsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error fetching credit cards"))
                .verify();
    }
    @Test
    void getCreditsByCustomer_Success() {
        // Arrange
        String customerId = "123";
        List<Credit> expectedCredits = Arrays.asList(
                createCredit("1", customerId),
                createCredit("2", customerId)
        );
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credits/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Credit>> response = new BaseResponse<>();
        response.setData(expectedCredits);
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditsByCustomer(customerId))
                .expectNextMatches(credits ->
                        credits.size() == 2 &&
                                credits.get(0).getCustomerId().equals(customerId) &&
                                credits.get(1).getCustomerId().equals(customerId))
                .verifyComplete();
    }
    @Test
    void getCreditsByCustomer_WhenEmptyResponse_ShouldReturnEmptyList() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credits/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        BaseResponse<List<Credit>> response = new BaseResponse<>();
        response.setData(Collections.emptyList());
        response.setStatus(200);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditsByCustomer(customerId))
                .expectNext(Collections.emptyList())
                .verifyComplete();
    }
    @Test
    void getCreditsByCustomer_WhenError_ShouldPropagateError() {
        // Arrange
        String customerId = "123";
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/credits/customer/{customerId}", customerId))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Error fetching credits")));
        // Act & Assert
        StepVerifier.create(creditClientService.getCreditsByCustomer(customerId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error fetching credits"))
                .verify();
    }
    private CreditCard createCreditCard(String id, String customerId) {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(id);
        creditCard.setCustomerId(customerId);
        creditCard.setCardType(CreditCardType.PERSONAL_CREDIT_CARD);
        creditCard.setCreditLimit(new BigDecimal("10000.0"));
        creditCard.setAvailableBalance(new BigDecimal("10000.0"));
        creditCard.setStatus("ACTIVE");
        creditCard.setCreatedAt(LocalDateTime.now());
        return creditCard;
    }
    private Credit createCredit(String id, String customerId) {
        Credit credit = new Credit();
        credit.setId(id);
        credit.setCustomerId(customerId);
        credit.setCreditType(CreditType.PERSONAL);
        credit.setAmount(new BigDecimal("5000.0"));
        credit.setRemainingBalance(new BigDecimal("5000.0"));
        credit.setInterestRate(new BigDecimal("0.12"));
        credit.setCreatedAt(LocalDateTime.now());
        return credit;
    }
}