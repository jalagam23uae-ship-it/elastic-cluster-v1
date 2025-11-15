package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.InvestigationStatus;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Investigation entity - payment investigation requests (camt.028/camt.029)
 */
@Table("investigations")
public class Investigation {

    @Id
    private UUID id;

    @Column("investigation_id")
    private String investigationId;

    @Column("investigation_status")
    private InvestigationStatus investigationStatus;

    @Column("investigation_type")
    private String investigationType;  // MISSING_PAYMENT, DUPLICATE_PAYMENT, etc.

    @Column("original_message_id")
    private UUID originalMessageId;

    @Column("original_uetr")
    private UUID originalUetr;

    @Column("original_end_to_end_id")
    private String originalEndToEndId;

    @Column("original_amount")
    private BigDecimal originalAmount;

    @Column("original_currency")
    private String originalCurrency;

    @Column("requester_bic")
    private String requesterBic;

    @Column("responder_bic")
    private String responderBic;

    @Column("request_message_id")
    private UUID requestMessageId;

    @Column("response_message_id")
    private UUID responseMessageId;

    @Column("description")
    private String description;

    @Column("resolution")
    private String resolution;

    @Column("priority")
    private String priority;  // LOW, MEDIUM, HIGH, URGENT

    @Column("opened_at")
    private Instant openedAt;

    @Column("responded_at")
    private Instant respondedAt;

    @Column("resolved_at")
    private Instant resolvedAt;

    @Column("closed_at")
    private Instant closedAt;

    @Column("sla_deadline")
    private Instant slaDeadline;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors
    public Investigation() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }
    
    public InvestigationStatus getInvestigationStatus() { return investigationStatus; }
    public void setInvestigationStatus(InvestigationStatus investigationStatus) { this.investigationStatus = investigationStatus; }
    
    public String getInvestigationType() { return investigationType; }
    public void setInvestigationType(String investigationType) { this.investigationType = investigationType; }
    
    public UUID getOriginalMessageId() { return originalMessageId; }
    public void setOriginalMessageId(UUID originalMessageId) { this.originalMessageId = originalMessageId; }
    
    public UUID getOriginalUetr() { return originalUetr; }
    public void setOriginalUetr(UUID originalUetr) { this.originalUetr = originalUetr; }
    
    public String getOriginalEndToEndId() { return originalEndToEndId; }
    public void setOriginalEndToEndId(String originalEndToEndId) { this.originalEndToEndId = originalEndToEndId; }
    
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    
    public String getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(String originalCurrency) { this.originalCurrency = originalCurrency; }
    
    public String getRequesterBic() { return requesterBic; }
    public void setRequesterBic(String requesterBic) { this.requesterBic = requesterBic; }
    
    public String getResponderBic() { return responderBic; }
    public void setResponderBic(String responderBic) { this.responderBic = responderBic; }
    
    public UUID getRequestMessageId() { return requestMessageId; }
    public void setRequestMessageId(UUID requestMessageId) { this.requestMessageId = requestMessageId; }
    
    public UUID getResponseMessageId() { return responseMessageId; }
    public void setResponseMessageId(UUID responseMessageId) { this.responseMessageId = responseMessageId; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    
    public Instant getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(Instant slaDeadline) { this.slaDeadline = slaDeadline; }
    
    public Json getMetadata() { return metadata; }
    public void setMetadata(Json metadata) { this.metadata = metadata; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
