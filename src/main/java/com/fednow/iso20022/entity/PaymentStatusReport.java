package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.PaymentStatusCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * PaymentStatusReport entity - stores payment status information (ACCP, RJCT, PDNG)
 */
@Table("payment_status_reports")
public class PaymentStatusReport {

    @Id
    private UUID id;

    @Column("message_id")
    private UUID messageId;

    @Column("original_message_id")
    private UUID originalMessageId;

    @Column("status_code")
    private PaymentStatusCode statusCode;

    @Column("reason_code")
    private String reasonCode;

    @Column("reason_description")
    private String reasonDescription;

    @Column("acceptance_datetime")
    private Instant acceptanceDatetime;

    @Column("clearing_system_reference")
    private String clearingSystemReference;

    @Column("created_at")
    private Instant createdAt;

    // Constructors
    public PaymentStatusReport() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    
    public UUID getOriginalMessageId() { return originalMessageId; }
    public void setOriginalMessageId(UUID originalMessageId) { this.originalMessageId = originalMessageId; }
    
    public PaymentStatusCode getStatusCode() { return statusCode; }
    public void setStatusCode(PaymentStatusCode statusCode) { this.statusCode = statusCode; }
    
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    
    public Instant getAcceptanceDatetime() { return acceptanceDatetime; }
    public void setAcceptanceDatetime(Instant acceptanceDatetime) { this.acceptanceDatetime = acceptanceDatetime; }
    
    public String getClearingSystemReference() { return clearingSystemReference; }
    public void setClearingSystemReference(String clearingSystemReference) { this.clearingSystemReference = clearingSystemReference; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
