package com.fednow.iso20022.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ConverterMetrics entity - performance metrics for converters
 */
@Table("converter_metrics")
public class ConverterMetrics {

    @Id
    private UUID id;

    @Column("converter_name")
    private String converterName;

    @Column("execution_count")
    private Long executionCount;

    @Column("success_count")
    private Long successCount;

    @Column("failure_count")
    private Long failureCount;

    @Column("avg_execution_time_ms")
    private BigDecimal avgExecutionTimeMs;

    @Column("min_execution_time_ms")
    private Integer minExecutionTimeMs;

    @Column("max_execution_time_ms")
    private Integer maxExecutionTimeMs;

    @Column("last_execution_at")
    private Instant lastExecutionAt;

    @Column("date")
    private LocalDate date;

    // Constructors
    public ConverterMetrics() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getConverterName() { return converterName; }
    public void setConverterName(String converterName) { this.converterName = converterName; }
    
    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }
    
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    
    public BigDecimal getAvgExecutionTimeMs() { return avgExecutionTimeMs; }
    public void setAvgExecutionTimeMs(BigDecimal avgExecutionTimeMs) { this.avgExecutionTimeMs = avgExecutionTimeMs; }
    
    public Integer getMinExecutionTimeMs() { return minExecutionTimeMs; }
    public void setMinExecutionTimeMs(Integer minExecutionTimeMs) { this.minExecutionTimeMs = minExecutionTimeMs; }
    
    public Integer getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
    public void setMaxExecutionTimeMs(Integer maxExecutionTimeMs) { this.maxExecutionTimeMs = maxExecutionTimeMs; }
    
    public Instant getLastExecutionAt() { return lastExecutionAt; }
    public void setLastExecutionAt(Instant lastExecutionAt) { this.lastExecutionAt = lastExecutionAt; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
