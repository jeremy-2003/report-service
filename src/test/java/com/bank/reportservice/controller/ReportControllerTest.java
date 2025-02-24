package com.bank.reportservice.controller;
import com.bank.reportservice.dto.*;
import com.bank.reportservice.model.transaction.ProductCategory;
import com.bank.reportservice.model.transaction.ProductSubType;
import com.bank.reportservice.model.transaction.TransactionType;
import com.bank.reportservice.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ReportControllerTest {
    @Mock
    private ReportService reportService;
    @InjectMocks
    private ReportController reportController;
    private WebTestClient webTestClient;
    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient
                .bindToController(reportController)
                .build();
    }
    @Test
    void getCustomerBalances_WhenSuccess_ShouldReturnBalances() {
        // Arrange
        String customerId = "123";
        CustomerBalances balances = CustomerBalances.builder()
                .customerId(customerId)
                .products(Arrays.asList(
                        createProductBalance("1", ProductCategory.ACCOUNT),
                        createProductBalance("2", ProductCategory.CREDIT_CARD)
                ))
                .build();
        when(reportService.getCustomerBalances(customerId))
                .thenReturn(Mono.just(balances));
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/balances/customer/{customerId}", customerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Customer balances retrieved successfully")
                .jsonPath("$.data.customerId").isEqualTo(customerId)
                .jsonPath("$.data.products.length()").isEqualTo(2);
    }
    @Test
    void getCustomerBalances_WhenEmpty_ShouldReturnNotFound() {
        // Arrange
        String customerId = "123";
        when(reportService.getCustomerBalances(customerId))
                .thenReturn(Mono.empty());
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/balances/customer/{customerId}", customerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("No balances found for customer")
                .jsonPath("$.data").isEmpty();
    }
    @Test
    void getCustomerBalances_WhenError_ShouldReturnInternalServerError() {
        // Arrange
        String customerId = "123";
        when(reportService.getCustomerBalances(customerId))
                .thenReturn(Mono.error(new RuntimeException("Service error")));
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/balances/customer/{customerId}", customerId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .jsonPath("$.message").isEqualTo("Error retrieving balances");
    }
    @Test
    void getProductMovements_WhenSuccess_ShouldReturnMovements() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        List<ProductMovement> movements = Arrays.asList(
                createProductMovement("1", customerId, productId),
                createProductMovement("2", customerId, productId)
        );
        when(reportService.getProductMovements(customerId, productId))
                .thenReturn(Mono.just(movements));
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/movements/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.OK.value())
                .jsonPath("$.message").isEqualTo("Product movements retrieved successfully")
                .jsonPath("$.data.length()").isEqualTo(2);
    }
    @Test
    void getProductMovements_WhenEmpty_ShouldReturnNotFound() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        when(reportService.getProductMovements(customerId, productId))
                .thenReturn(Mono.empty());
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/movements/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
                .jsonPath("$.message").isEqualTo("No movements found for product")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(0);
    }
    @Test
    void getProductMovements_WhenError_ShouldReturnInternalServerError() {
        // Arrange
        String customerId = "123";
        String productId = "456";
        when(reportService.getProductMovements(customerId, productId))
                .thenReturn(Mono.error(new RuntimeException("Service error")));
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/movements/customer/{customerId}/product/{productId}",
                        customerId, productId)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .jsonPath("$.message").isEqualTo("Error retrieving movements");
    }
    @Test
    void getMonthlyBalanceSummary_WhenSuccess_ShouldReturnSummary() {
        // Arrange
        String customerId = "123";
        List<DailyBalanceSummary> summaries = Arrays.asList(
                createDailyBalanceSummary("1", "ACCOUNT", "SAVINGS"),
                createDailyBalanceSummary("2", "CREDIT", "PERSONAL")
        );
        when(reportService.getMonthlyBalanceSummary(customerId))
                .thenReturn(Mono.just(summaries));
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/{customerId}/summary", customerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2);
    }
    @Test
    void getMonthlyBalanceSummary_WhenEmpty_ShouldReturnNotFound() {
        // Arrange
        String customerId = "123";
        when(reportService.getMonthlyBalanceSummary(customerId))
                .thenReturn(Mono.empty());
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/{customerId}/summary", customerId)
                .exchange()
                .expectStatus().isNotFound();
    }
    @Test
    void getTransactionSummary_WhenSuccess_ShouldReturnSummary() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        BaseResponse<List<CategorySummary>> response = BaseResponse.<List<CategorySummary>>builder()
                .status(HttpStatus.OK.value())
                .message("Success")
                .data(Arrays.asList(
                        createCategorySummary("ACCOUNT", 5, 500.0),
                        createCategorySummary("CREDIT", 3, 300.0)
                ))
                .build();
        when(reportService.fetchTransactionSummaryByDate(startDate, endDate))
                .thenReturn(Mono.just(response));
        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/reports/transactions/summary")
                        .queryParam("startDate", startDate.toString())
                        .queryParam("endDate", endDate.toString())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2);
    }
    @Test
    void getTransactionSummary_WhenInvalidDates_ShouldReturnBadRequest() {
        // Act & Assert
        webTestClient.get()
                .uri("/api/reports/transactions/summary")
                .exchange()
                .expectStatus().isBadRequest();
    }
    // Helper methods
    private ProductBalance createProductBalance(String id, ProductCategory category) {
        return ProductBalance.builder()
                .productId(id)
                .type(category)
                .subType(ProductSubType.SAVINGS)
                .availableBalance(BigDecimal.valueOf(1000))
                .build();
    }
    private ProductMovement createProductMovement(String id, String customerId, String productId) {
        return ProductMovement.builder()
                .transactionId(id)
                .date(LocalDateTime.now())
                .type(TransactionType.DEPOSIT)
                .amount(BigDecimal.valueOf(100))
                .productCategory(ProductCategory.ACCOUNT)
                .productSubType(ProductSubType.SAVINGS)
                .build();
    }
    private DailyBalanceSummary createDailyBalanceSummary(String productId, String type, String subType) {
        return new DailyBalanceSummary(
                productId,
                type,
                subType,
                BigDecimal.valueOf(1000)
        );
    }
    private CategorySummary createCategorySummary(String category, int quantity, double amount) {
        return new CategorySummary(
                category,
                quantity,
                amount
        );
    }
}
