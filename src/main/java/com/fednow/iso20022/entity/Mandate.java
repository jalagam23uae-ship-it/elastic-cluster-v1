package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.MandateStatus;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mandate entity - stores direct debit mandates
 */
@Table("mandates")
public class Mandate {

    @Id
    private UUID id;

    @Column("mandate_id")
    private String mandateId;

    @Column("mandate_status")
    private MandateStatus mandateStatus;

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

    @Column("mandate_type")
    private String mandateType;  // RCUR, OOFF, FNAL, FRST

    @Column("max_amount")
    private BigDecimal maxAmount;

    @Column("frequency")
    private String frequency;  // MONTHLY, WEEKLY, DAILY

    @Column("signature_date")
    private LocalDate signatureDate;

    @Column("activation_date")
    private LocalDate activationDate;

    @Column("expiration_date")
    private LocalDate expirationDate;

    @Column("last_used_date")
    private LocalDate lastUsedDate;

    @Column("usage_count")
    private Integer usageCount;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors, getters, setters (omitted for brevity - same pattern as previous entities)

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMandateId() { return mandateId; }
    public void setMandateId(String mandateId) { this.mandateId = mandateId; }
    public MandateStatus getMandateStatus() { return mandateStatus; }
    public void setMandateStatus(MandateStatus mandateStatus) { this.mandateStatus = mandateStatus; }
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
    public String getMandateType() { return mandateType; }
    public void setMandateType(String mandateType) { this.mandateType = mandateType; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getSignatureDate() { return signatureDate; }
    public void setSignatureDate(LocalDate signatureDate) { this.signatureDate = signatureDate; }
    public LocalDate getActivationDate() { return activationDate; }
    public void setActivationDate(LocalDate activationDate) { this.activationDate = activationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }
    public LocalDate getLastUsedDate() { return lastUsedDate; }
    public void setLastUsedDate(LocalDate lastUsedDate) { this.lastUsedDate = lastUsedDate; }
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
    public Json getMetadata() { return metadata; }
    public void setMetadata(Json metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
