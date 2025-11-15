package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.ReversalStatus;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Reversal entity - tracks payment reversals with 15-second FedNow window
 */
@Table("reversals")
public class Reversal {

    @Id
    private UUID id;

    @Column("reversal_id")
    private String reversalId;

    @Column("original_message_id")
    private UUID originalMessageId;

    @Column("reversal_message_id")
    private UUID reversalMessageId;

    @Column("reversal_status")
    private ReversalStatus reversalStatus;

    @Column("reversal_reason_code")
    private String reversalReasonCode;

    @Column("reversal_reason_description")
    private String reversalReasonDescription;

    @Column("original_uetr")
    private UUID originalUetr;

    @Column("original_end_to_end_id")
    private String originalEndToEndId;

    @Column("original_amount")
    private BigDecimal originalAmount;

    @Column("original_currency")
    private String originalCurrency;

    @Column("reversal_requested_at")
    private Instant reversalRequestedAt;

    @Column("reversal_processed_at")
    private Instant reversalProcessedAt;

    @Column("reversal_deadline")
    private Instant reversalDeadline;  // 15 seconds for FedNow

    @Column("within_window")
    private Boolean withinWindow;

    @Column("initiated_by")
    private String initiatedBy;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors
    public Reversal() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getReversalId() { return reversalId; }
    public void setReversalId(String reversalId) { this.reversalId = reversalId; }
    
    public UUID getOriginalMessageId() { return originalMessageId; }
    public void setOriginalMessageId(UUID originalMessageId) { this.originalMessageId = originalMessageId; }
    
    public UUID getReversalMessageId() { return reversalMessageId; }
    public void setReversalMessageId(UUID reversalMessageId) { this.reversalMessageId = reversalMessageId; }
    
    public ReversalStatus getReversalStatus() { return reversalStatus; }
    public void setReversalStatus(ReversalStatus reversalStatus) { this.reversalStatus = reversalStatus; }
    
    public String getReversalReasonCode() { return reversalReasonCode; }
    public void setReversalReasonCode(String reversalReasonCode) { this.reversalReasonCode = reversalReasonCode; }
    
    public String getReversalReasonDescription() { return reversalReasonDescription; }
    public void setReversalReasonDescription(String reversalReasonDescription) { this.reversalReasonDescription = reversalReasonDescription; }
    
    public UUID getOriginalUetr() { return originalUetr; }
    public void setOriginalUetr(UUID originalUetr) { this.originalUetr = originalUetr; }
    
    public String getOriginalEndToEndId() { return originalEndToEndId; }
    public void setOriginalEndToEndId(String originalEndToEndId) { this.originalEndToEndId = originalEndToEndId; }
    
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    
    public String getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(String originalCurrency) { this.originalCurrency = originalCurrency; }
    
    public Instant getReversalRequestedAt() { return reversalRequestedAt; }
    public void setReversalRequestedAt(Instant reversalRequestedAt) { this.reversalRequestedAt = reversalRequestedAt; }
    
    public Instant getReversalProcessedAt() { return reversalProcessedAt; }
    public void setReversalProcessedAt(Instant reversalProcessedAt) { this.reversalProcessedAt = reversalProcessedAt; }
    
    public Instant getReversalDeadline() { return reversalDeadline; }
    public void setReversalDeadline(Instant reversalDeadline) { this.reversalDeadline = reversalDeadline; }
    
    public Boolean getWithinWindow() { return withinWindow; }
    public void setWithinWindow(Boolean withinWindow) { this.withinWindow = withinWindow; }
    
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    
    public Json getMetadata() { return metadata; }
    public void setMetadata(Json metadata) { this.metadata = metadata; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
