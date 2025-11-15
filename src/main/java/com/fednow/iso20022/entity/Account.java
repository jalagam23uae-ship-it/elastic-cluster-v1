package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.AccountStatus;
import com.fednow.iso20022.entity.enums.AccountType;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Account entity - represents customer and institutional accounts
 */
@Table("accounts")
public class Account {

    @Id
    private UUID id;

    @Column("account_number")
    private String accountNumber;  // IBAN format

    @Column("account_type")
    private AccountType accountType;

    @Column("account_status")
    private AccountStatus accountStatus;

    @Column("account_holder_name")
    private String accountHolderName;

    @Column("account_holder_id")
    private String accountHolderId;  // Tax ID or customer ID

    @Column("institution_bic")
    private String institutionBic;

    @Column("currency")
    private String currency;

    @Column("balance")
    private BigDecimal balance;

    @Column("available_balance")
    private BigDecimal availableBalance;

    @Column("overdraft_limit")
    private BigDecimal overdraftLimit;

    @Column("opened_date")
    private LocalDate openedDate;

    @Column("closed_date")
    private LocalDate closedDate;

    @Column("last_transaction_date")
    private Instant lastTransactionDate;

    @Column("ofac_status")
    private String ofacStatus;

    @Column("ofac_last_check")
    private Instant ofacLastCheck;

    @Column("fraud_score")
    private Integer fraudScore;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructors
    public Account() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountHolderId() {
        return accountHolderId;
    }

    public void setAccountHolderId(String accountHolderId) {
        this.accountHolderId = accountHolderId;
    }

    public String getInstitutionBic() {
        return institutionBic;
    }

    public void setInstitutionBic(String institutionBic) {
        this.institutionBic = institutionBic;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(LocalDate openedDate) {
        this.openedDate = openedDate;
    }

    public LocalDate getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(LocalDate closedDate) {
        this.closedDate = closedDate;
    }

    public Instant getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(Instant lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public String getOfacStatus() {
        return ofacStatus;
    }

    public void setOfacStatus(String ofacStatus) {
        this.ofacStatus = ofacStatus;
    }

    public Instant getOfacLastCheck() {
        return ofacLastCheck;
    }

    public void setOfacLastCheck(Instant ofacLastCheck) {
        this.ofacLastCheck = ofacLastCheck;
    }

    public Integer getFraudScore() {
        return fraudScore;
    }

    public void setFraudScore(Integer fraudScore) {
        this.fraudScore = fraudScore;
    }

    public Json getMetadata() {
        return metadata;
    }

    public void setMetadata(Json metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
