package com.fednow.iso20022.service;

import com.fednow.iso20022.entity.Conversion;
import com.fednow.iso20022.entity.ConverterMetrics;
import com.fednow.iso20022.repository.ConversionRepository;
import com.fednow.iso20022.repository.ConverterMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for converter orchestration, metrics tracking, and conversion auditing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConverterOrchestrationService {

    private final ConversionRepository conversionRepository;
    private final ConverterMetricsRepository converterMetricsRepository;

    /**
     * Record a successful conversion
     */
    public Mono<Conversion> recordConversion(
            UUID sourceMessageId,
            UUID targetMessageId,
            String converterName,
            long executionTimeMs) {
        
        log.debug("Recording conversion: converter={}, sourceId={}, targetId={}, time={}ms", 
                converterName, sourceMessageId, targetMessageId, executionTimeMs);
        
        Conversion conversion = new Conversion();
        conversion.setSourceMessageId(sourceMessageId);
        conversion.setTargetMessageId(targetMessageId);
        conversion.setConverterName(converterName);
        conversion.setConversionTimeMs((int) executionTimeMs);
        conversion.setStatus("SUCCESS");
        conversion.setCreatedAt(Instant.now());
        
        return conversionRepository.save(conversion)
                .flatMap(saved -> {
                    log.info("Conversion recorded: id={}, converter={}, time={}ms", 
                            saved.getId(), converterName, executionTimeMs);
                    return updateConverterMetrics(converterName, executionTimeMs, true)
                            .thenReturn(saved);
                })
                .doOnError(e -> log.error("Failed to record conversion: converter={}", converterName, e));
    }

    /**
     * Record a failed conversion
     */
    public Mono<Conversion> recordFailedConversion(
            UUID sourceMessageId,
            String converterName,
            long executionTimeMs,
            String errorMessage) {
        
        log.debug("Recording failed conversion: converter={}, sourceId={}, error={}", 
                converterName, sourceMessageId, errorMessage);
        
        Conversion conversion = new Conversion();
        conversion.setSourceMessageId(sourceMessageId);
        conversion.setConverterName(converterName);
        conversion.setConversionTimeMs((int) executionTimeMs);
        conversion.setStatus("FAILED");
        conversion.setErrorMessage(errorMessage);
        conversion.setCreatedAt(Instant.now());
        
        return conversionRepository.save(conversion)
                .flatMap(saved -> {
                    log.warn("Failed conversion recorded: id={}, converter={}, error={}", 
                            saved.getId(), converterName, errorMessage);
                    return updateConverterMetrics(converterName, executionTimeMs, false)
                            .thenReturn(saved);
                })
                .doOnError(e -> log.error("Failed to record failed conversion: converter={}", converterName, e));
    }

    /**
     * Update converter metrics (daily aggregates)
     */
    private Mono<ConverterMetrics> updateConverterMetrics(String converterName, long executionTimeMs, boolean success) {
        LocalDate today = LocalDate.now();
        
        return converterMetricsRepository.findByConverterNameAndDate(converterName, today)
                .switchIfEmpty(createNewMetrics(converterName, today))
                .flatMap(metrics -> {
                    // Update counts
                    metrics.setExecutionCount(metrics.getExecutionCount() + 1);
                    if (success) {
                        metrics.setSuccessCount(metrics.getSuccessCount() + 1);
                    } else {
                        metrics.setFailureCount(metrics.getFailureCount() + 1);
                    }
                    
                    // Update execution times
                    int execTime = (int) executionTimeMs;
                    if (metrics.getMinExecutionTimeMs() == 0 || execTime < metrics.getMinExecutionTimeMs()) {
                        metrics.setMinExecutionTimeMs(execTime);
                    }
                    if (execTime > metrics.getMaxExecutionTimeMs()) {
                        metrics.setMaxExecutionTimeMs(execTime);
                    }
                    
                    // Update average execution time
                    BigDecimal currentAvg = metrics.getAvgExecutionTimeMs();
                    BigDecimal currentCount = BigDecimal.valueOf(metrics.getExecutionCount());
                    BigDecimal newAvg = currentAvg.multiply(currentCount.subtract(BigDecimal.ONE))
                            .add(BigDecimal.valueOf(execTime))
                            .divide(currentCount, 2, RoundingMode.HALF_UP);
                    metrics.setAvgExecutionTimeMs(newAvg);
                    
                    metrics.setLastExecutionAt(Instant.now());
                    
                    return converterMetricsRepository.save(metrics);
                })
                .doOnSuccess(saved -> log.debug("Converter metrics updated: converter={}, executions={}, successRate={}", 
                        converterName, saved.getExecutionCount(), 
                        calculateSuccessRate(saved.getSuccessCount(), saved.getExecutionCount())))
                .doOnError(e -> log.error("Failed to update converter metrics: converter={}", converterName, e));
    }

    /**
     * Create new metrics entry for a converter
     */
    private Mono<ConverterMetrics> createNewMetrics(String converterName, LocalDate date) {
        ConverterMetrics metrics = new ConverterMetrics();
        metrics.setConverterName(converterName);
        metrics.setDate(date);
        metrics.setExecutionCount(0L);
        metrics.setSuccessCount(0L);
        metrics.setFailureCount(0L);
        metrics.setAvgExecutionTimeMs(BigDecimal.ZERO);
        metrics.setMinExecutionTimeMs(0);
        metrics.setMaxExecutionTimeMs(0);
        return Mono.just(metrics);
    }

    /**
     * Get converter metrics for a specific date
     */
    public Mono<ConverterMetrics> getConverterMetrics(String converterName, LocalDate date) {
        log.debug("Retrieving metrics for converter: {}, date: {}", converterName, date);
        return converterMetricsRepository.findByConverterNameAndDate(converterName, date);
    }

    /**
     * Get converter metrics for a date range
     */
    public Flux<ConverterMetrics> getConverterMetricsRange(String converterName, LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving metrics for converter: {}, range: {} to {}", converterName, startDate, endDate);
        return converterMetricsRepository.findByConverterNameAndDateRange(converterName, startDate, endDate);
    }

    /**
     * Get conversion history for a source message
     */
    public Flux<Conversion> getConversionHistory(UUID sourceMessageId) {
        log.debug("Retrieving conversion history for message: {}", sourceMessageId);
        return conversionRepository.findBySourceMessageId(sourceMessageId);
    }

    /**
     * Get recent failed conversions
     */
    public Flux<Conversion> getRecentFailedConversions(int limit) {
        log.debug("Retrieving recent failed conversions: limit={}", limit);
        return conversionRepository.findFailedConversions(limit);
    }

    /**
     * Calculate converter success rate
     */
    public Mono<Double> getConverterSuccessRate(String converterName) {
        log.debug("Calculating success rate for converter: {}", converterName);
        return converterMetricsRepository.calculateSuccessRate(converterName);
    }

    /**
     * Get total executions for a converter
     */
    public Mono<Long> getTotalExecutions(String converterName) {
        log.debug("Retrieving total executions for converter: {}", converterName);
        return converterMetricsRepository.getTotalExecutions(converterName);
    }

    /**
     * Get converters with high failure rates
     */
    public Flux<ConverterMetrics> getProblematicConverters(double failureThreshold) {
        log.debug("Retrieving converters with failure rate > {}", failureThreshold);
        return converterMetricsRepository.findConvertersWithHighFailureRate(failureThreshold);
    }

    /**
     * Calculate success rate percentage
     */
    private double calculateSuccessRate(long successCount, long totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return (successCount * 100.0) / totalCount;
    }
}
