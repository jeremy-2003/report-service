package com.bank.reportservice.controller;

import com.bank.reportservice.dto.BaseResponse;
import com.bank.reportservice.dto.CustomerBalances;
import com.bank.reportservice.dto.ProductMovement;
import com.bank.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
}
