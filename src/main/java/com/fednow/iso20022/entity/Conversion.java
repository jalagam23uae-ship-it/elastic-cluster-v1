package com.fednow.iso20022.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Conversion entity - tracks message-to-message conversions
 */
@Table("conversions")
public class Conversion {

    @Id
    private UUID id;

    @Column("source_message_id")
    private UUID sourceMessageId;

    @Column("target_message_id")
    private UUID targetMessageId;

    @Column("converter_name")
    private String converterName;

    @Column("conversion_time_ms")
    private Integer conversionTimeMs;

    @Column("status")
    private String status;  // SUCCESS, FAILED, PARTIAL

    @Column("error_message")
    private String errorMessage;

    @Column("created_at")
    private Instant createdAt;

    // Constructors
    public Conversion() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(UUID sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    
    public UUID getTargetMessageId() { return targetMessageId; }
    public void setTargetMessageId(UUID targetMessageId) { this.targetMessageId = targetMessageId; }
    
    public String getConverterName() { return converterName; }
    public void setConverterName(String converterName) { this.converterName = converterName; }
    
    public Integer getConversionTimeMs() { return conversionTimeMs; }
    public void setConversionTimeMs(Integer conversionTimeMs) { this.conversionTimeMs = conversionTimeMs; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
