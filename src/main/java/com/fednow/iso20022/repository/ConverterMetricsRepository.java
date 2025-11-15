package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.ConverterMetrics;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for ConverterMetrics entities
 */
@Repository
public interface ConverterMetricsRepository extends ReactiveCrudRepository<ConverterMetrics, UUID> {

    /**
     * Find metrics by converter name
     */
    Flux<ConverterMetrics> findByConverterName(String converterName);

    /**
     * Find metrics by date
     */
    Flux<ConverterMetrics> findByDate(LocalDate date);

    /**
     * Find metrics by converter name and date
     */
    Mono<ConverterMetrics> findByConverterNameAndDate(String converterName, LocalDate date);

    /**
     * Find metrics for date range
     */
    @Query("SELECT * FROM converter_metrics WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    Flux<ConverterMetrics> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Find metrics for converter in date range
     */
    @Query("SELECT * FROM converter_metrics WHERE converter_name = :converterName AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    Flux<ConverterMetrics> findByConverterNameAndDateRange(String converterName, LocalDate startDate, LocalDate endDate);

    /**
     * Get total executions for converter
     */
    @Query("SELECT COALESCE(SUM(execution_count), 0) FROM converter_metrics WHERE converter_name = :converterName")
    Mono<Long> getTotalExecutions(String converterName);

    /**
     * Get total success count for converter
     */
    @Query("SELECT COALESCE(SUM(success_count), 0) FROM converter_metrics WHERE converter_name = :converterName")
    Mono<Long> getTotalSuccesses(String converterName);

    /**
     * Get total failure count for converter
     */
    @Query("SELECT COALESCE(SUM(failure_count), 0) FROM converter_metrics WHERE converter_name = :converterName")
    Mono<Long> getTotalFailures(String converterName);

    /**
     * Calculate success rate for converter
     */
    @Query("SELECT CASE WHEN SUM(execution_count) > 0 THEN (SUM(success_count)::float / SUM(execution_count)::float * 100) ELSE 0 END FROM converter_metrics WHERE converter_name = :converterName")
    Mono<Double> calculateSuccessRate(String converterName);

    /**
     * Find converters with high failure rates
     */
    @Query("SELECT * FROM converter_metrics WHERE failure_count > 0 AND (failure_count::float / execution_count::float) > :threshold ORDER BY date DESC")
    Flux<ConverterMetrics> findConvertersWithHighFailureRate(double threshold);
}
