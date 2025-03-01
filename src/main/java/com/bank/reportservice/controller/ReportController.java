package com.bank.reportservice.controller;

import com.bank.reportservice.dto.*;
import com.bank.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Slf4j
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    @GetMapping("/balances/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<CustomerBalances>>> getCustomerBalances(
            @PathVariable String customerId) {
        return reportService.getCustomerBalances(customerId)
                .map(balances -> ResponseEntity.ok(BaseResponse.<CustomerBalances>builder()
                        .status(HttpStatus.OK.value())
                        .message("Customer balances retrieved successfully")
                        .data(balances)
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(BaseResponse.<CustomerBalances>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No balances found for customer")
                        .build()))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving customer balances", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<CustomerBalances>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving balances")
                                    .build()));
                });
    }
    @GetMapping("/resume/customer/{customerId}")
    public Mono<ResponseEntity<BaseResponse<CustomerBalances>>> getResumeByProductAndUserAndDates(
            @PathVariable String customerId,
            @RequestParam String typeProduct,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getResumeByProductAndUserAndDates(typeProduct, customerId, startDate, endDate)
                .map(balances -> ResponseEntity.ok(BaseResponse.<CustomerBalances>builder()
                        .status(HttpStatus.OK.value())
                        .message("Resume for customer, products and dates retrieved successfully")
                        .data(balances)
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(BaseResponse.<CustomerBalances>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No resume for customer, products and dates found")
                        .build()))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving resume", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<CustomerBalances>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving resume")
                                    .build()));
                });
    }
    @GetMapping("/movements/customer/{customerId}/product/{productId}")
    public Mono<ResponseEntity<BaseResponse<List<ProductMovement>>>> getProductMovements(
            @PathVariable String customerId,
            @PathVariable String productId) {
        return reportService.getProductMovements(customerId, productId)
                .map(movements -> ResponseEntity.ok(BaseResponse.<List<ProductMovement>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Product movements retrieved successfully")
                        .data(movements)
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(BaseResponse.<List<ProductMovement>>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No movements found for product")
                        .data(Collections.emptyList())
                        .build()))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving product movements", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<List<ProductMovement>>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving movements")
                                    .build()));
                });
    }
    @GetMapping("/movements/customer/{customerId}/card/{cardId}/recent")
    public Mono<ResponseEntity<BaseResponse<List<ProductMovement>>>> getRecentCardMovements(
            @PathVariable String customerId,
            @PathVariable String cardId,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.getRecentCardMovements(customerId, cardId, limit)
                .map(movements -> ResponseEntity.ok(BaseResponse.<List<ProductMovement>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Recent " + cardId + " card movements retrieved successfully")
                        .data(movements)
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(BaseResponse.<List<ProductMovement>>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No recent movements found for " + cardId + " card")
                        .data(Collections.emptyList())
                        .build()))
                .onErrorResume(Exception.class, e -> {
                    log.error("Error retrieving recent card movements", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(BaseResponse.<List<ProductMovement>>builder()
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                    .message("Error retrieving recent card movements")
                                    .build()));
                });
    }
    @GetMapping("/{customerId}/summary")
    public Mono<ResponseEntity<List<DailyBalanceSummary>>> getMonthlyBalanceSummary(@PathVariable String customerId) {
        return reportService.getMonthlyBalanceSummary(customerId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    @GetMapping("/transactions/summary")
    public Mono<ResponseEntity<BaseResponse<List<CategorySummary>>>> getTransactionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.fetchTransactionSummaryByDate(startDate, endDate)
                .map(ResponseEntity::ok);
    }
}
