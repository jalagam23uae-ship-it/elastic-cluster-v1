package com.fednow.iso20022.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * ValidationError entity - records validation errors for messages
 */
@Table("validation_errors")
public class ValidationError {

    @Id
    private UUID id;

    @Column("message_id")
    private UUID messageId;

    @Column("error_code")
    private String errorCode;

    @Column("error_category")
    private String errorCategory;

    @Column("severity")
    private String severity;  // INFO, WARNING, ERROR, CRITICAL

    @Column("field_path")
    private String fieldPath;

    @Column("error_description")
    private String errorDescription;

    @Column("created_at")
    private Instant createdAt;

    // Constructors
    public ValidationError() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    
    public String getErrorCategory() { return errorCategory; }
    public void setErrorCategory(String errorCategory) { this.errorCategory = errorCategory; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getFieldPath() { return fieldPath; }
    public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }
    
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
