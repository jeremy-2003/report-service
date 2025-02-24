package com.bank.reportservice.schedualed;

import com.bank.reportservice.scheduled.DailyBalanceScheduler;
import com.bank.reportservice.service.DailyBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DailyBalanceSchedulerTest {
    @Mock
    private DailyBalanceService dailyBalanceService;
    private DailyBalanceScheduler scheduler;
    @BeforeEach
    void setUp() {
        scheduler = new DailyBalanceScheduler(dailyBalanceService);
    }
    @Test
    void executeDailyBalanceJob_Success() {
        // Arrange
        when(dailyBalanceService.processDailyBalances())
                .thenReturn(Mono.empty());
        // Act
        scheduler.executeDailyBalanceJob();
        // Assert
        verify(dailyBalanceService, times(1)).processDailyBalances();
    }
    @Test
    void executeDailyBalanceJob_WhenServiceSucceeds_ShouldComplete() {
        // Arrange
        when(dailyBalanceService.processDailyBalances())
                .thenReturn(Mono.empty());
        // Act & Assert
        StepVerifier.create(dailyBalanceService.processDailyBalances())
                .verifyComplete();
    }
    @Test
    void executeDailyBalanceJob_WhenServiceFails_ShouldNotThrowException() {
        // Arrange
        when(dailyBalanceService.processDailyBalances())
                .thenReturn(Mono.error(new RuntimeException("Process failed")));
        // Act
        scheduler.executeDailyBalanceJob();
        // Assert
        verify(dailyBalanceService, times(1)).processDailyBalances();
    }
    @Test
    void verifyScheduledAnnotation() {
        // Verify that the method has the correct @Scheduled annotation
        Scheduled annotation = null;
        try {
            annotation = DailyBalanceScheduler.class
                    .getMethod("executeDailyBalanceJob")
                    .getAnnotation(Scheduled.class);
        } catch (NoSuchMethodException e) {
            fail("Method executeDailyBalanceJob not found");
        }
        assertNotNull(annotation, "@Scheduled annotation not found");
        assertEquals("59 59 23 * * ?", annotation.cron(),
                "Incorrect cron expression");
    }
    @Test
    void verifyComponentAnnotation() {
        // Verify that the class has the @Component annotation
        Component annotation = DailyBalanceScheduler.class.getAnnotation(Component.class);
        assertNotNull(annotation, "@Component annotation not found");
    }
}
