package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.RfpStatus;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * RfpRequest entity - Request for Payment tracking (pain.013/pain.014)
 */
@Table("rfp_requests")
public class RfpRequest {

    @Id
    private UUID id;

    @Column("rfp_id")
    private String rfpId;

    @Column("rfp_status")
    private RfpStatus rfpStatus;

    @Column("creditor_name")
    private String creditorName;

    @Column("creditor_account")
    private String creditorAccount;

    @Column("creditor_bic")
    private String creditorBic;

    @Column("debtor_name")
    private String debtorName;

    @Column("debtor_account")
    private String debtorAccount;

    @Column("debtor_bic")
    private String debtorBic;

    @Column("requested_amount")
    private BigDecimal requestedAmount;

    @Column("currency")
    private String currency;

    @Column("due_date")
    private LocalDate dueDate;

    @Column("invoice_number")
    private String invoiceNumber;

    @Column("invoice_date")
    private LocalDate invoiceDate;

    @Column("remittance_information")
    private String remittanceInformation;

    @Column("request_message_id")
    private UUID requestMessageId;

    @Column("response_message_id")
    private UUID responseMessageId;

    @Column("related_payment_id")
    private UUID relatedPaymentId;

    @Column("requested_at")
    private Instant requestedAt;

    @Column("responded_at")
    private Instant respondedAt;

    @Column("expiration_date")
    private Instant expirationDate;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors
    public RfpRequest() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getRfpId() { return rfpId; }
    public void setRfpId(String rfpId) { this.rfpId = rfpId; }
    
    public RfpStatus getRfpStatus() { return rfpStatus; }
    public void setRfpStatus(RfpStatus rfpStatus) { this.rfpStatus = rfpStatus; }
    
    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    
    public String getCreditorAccount() { return creditorAccount; }
    public void setCreditorAccount(String creditorAccount) { this.creditorAccount = creditorAccount; }
    
    public String getCreditorBic() { return creditorBic; }
    public void setCreditorBic(String creditorBic) { this.creditorBic = creditorBic; }
    
    public String getDebtorName() { return debtorName; }
    public void setDebtorName(String debtorName) { this.debtorName = debtorName; }
    
    public String getDebtorAccount() { return debtorAccount; }
    public void setDebtorAccount(String debtorAccount) { this.debtorAccount = debtorAccount; }
    
    public String getDebtorBic() { return debtorBic; }
    public void setDebtorBic(String debtorBic) { this.debtorBic = debtorBic; }
    
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    
    public String getRemittanceInformation() { return remittanceInformation; }
    public void setRemittanceInformation(String remittanceInformation) { this.remittanceInformation = remittanceInformation; }
    
    public UUID getRequestMessageId() { return requestMessageId; }
    public void setRequestMessageId(UUID requestMessageId) { this.requestMessageId = requestMessageId; }
    
    public UUID getResponseMessageId() { return responseMessageId; }
    public void setResponseMessageId(UUID responseMessageId) { this.responseMessageId = responseMessageId; }
    
    public UUID getRelatedPaymentId() { return relatedPaymentId; }
    public void setRelatedPaymentId(UUID relatedPaymentId) { this.relatedPaymentId = relatedPaymentId; }
    
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }
    
    public Instant getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Instant expirationDate) { this.expirationDate = expirationDate; }
    
    public Json getMetadata() { return metadata; }
    public void setMetadata(Json metadata) { this.metadata = metadata; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
