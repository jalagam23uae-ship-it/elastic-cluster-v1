package com.fednow.iso20022.entity;

import com.fednow.iso20022.entity.enums.MessageStatus;
import com.fednow.iso20022.entity.enums.MessageType;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Message entity - represents all ISO 20022 messages in the system
 */
@Table("messages")
public class Message {

    @Id
    private UUID id;

    @Column("message_id")
    private String messageId;

    @Column("message_type")
    private MessageType messageType;

    @Column("uetr")
    private UUID uetr;

    @Column("end_to_end_id")
    private String endToEndId;

    @Column("transaction_id")
    private String transactionId;

    @Column("instruction_id")
    private String instructionId;

    @Column("original_message_id")
    private String originalMessageId;

    @Column("status")
    private MessageStatus status;

    @Column("direction")
    private String direction;  // INBOUND or OUTBOUND

    @Column("xml_content")
    private String xmlContent;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("processed_at")
    private Instant processedAt;

    @Column("instg_agent")
    private String instgAgent;

    @Column("instd_agent")
    private String instdAgent;

    @Column("debtor_name")
    private String debtorName;

    @Column("debtor_account")
    private String debtorAccount;

    @Column("creditor_name")
    private String creditorName;

    @Column("creditor_account")
    private String creditorAccount;

    @Column("amount")
    private BigDecimal amount;

    @Column("currency")
    private String currency;

    @Column("settlement_date")
    private LocalDate settlementDate;

    @Column("metadata")
    private Json metadata;

    // Constructors
    public Message() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public UUID getUetr() {
        return uetr;
    }

    public void setUetr(UUID uetr) {
        this.uetr = uetr;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getOriginalMessageId() {
        return originalMessageId;
    }

    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public String getInstgAgent() {
        return instgAgent;
    }

    public void setInstgAgent(String instgAgent) {
        this.instgAgent = instgAgent;
    }

    public String getInstdAgent() {
        return instdAgent;
    }

    public void setInstdAgent(String instdAgent) {
        this.instdAgent = instdAgent;
    }

    public String getDebtorName() {
        return debtorName;
    }

    public void setDebtorName(String debtorName) {
        this.debtorName = debtorName;
    }

    public String getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(String debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public String getCreditorName() {
        return creditorName;
    }

    public void setCreditorName(String creditorName) {
        this.creditorName = creditorName;
    }

    public String getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(String creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Json getMetadata() {
        return metadata;
    }

    public void setMetadata(Json metadata) {
        this.metadata = metadata;
    }
}
