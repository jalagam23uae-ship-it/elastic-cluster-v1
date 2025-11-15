package com.fednow.iso20022.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AccountTransaction entity - detailed transaction history for accounts
 */
@Table("account_transactions")
public class AccountTransaction {

    @Id
    private UUID id;

    @Column("account_id")
    private UUID accountId;

    @Column("message_id")
    private UUID messageId;

    @Column("transaction_type")
    private String transactionType;  // DEBIT, CREDIT, HOLD, RELEASE, FEE, REVERSAL

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("balance_before")
    private BigDecimal balanceBefore;

    @Column("balance_after")
    private BigDecimal balanceAfter;

    @Column("counterparty_name")
    private String counterpartyName;

    @Column("counterparty_account")
    private String counterpartyAccount;

    @Column("counterparty_bic")
    private String counterpartyBic;

    @Column("end_to_end_id")
    private String endToEndId;

    @Column("transaction_id")
    private String transactionId;

    @Column("uetr")
    private UUID uetr;

    @Column("description")
    private String description;

    @Column("booking_date")
    private LocalDate bookingDate;

    @Column("value_date")
    private LocalDate valueDate;

    @Column("transaction_time")
    private Instant transactionTime;

    @Column("metadata")
    private Json metadata;

    @Column("created_at")
    private Instant createdAt;

    // Constructors
    public AccountTransaction() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    
    public String getCounterpartyBic() { return counterpartyBic; }
    public void setCounterpartyBic(String counterpartyBic) { this.counterpartyBic = counterpartyBic; }
    
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public UUID getUetr() { return uetr; }
    public void setUetr(UUID uetr) { this.uetr = uetr; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }
    
    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }
    
    public Instant getTransactionTime() { return transactionTime; }
    public void setTransactionTime(Instant transactionTime) { this.transactionTime = transactionTime; }
    
    public Json getMetadata() { return metadata; }
    public void setMetadata(Json metadata) { this.metadata = metadata; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
