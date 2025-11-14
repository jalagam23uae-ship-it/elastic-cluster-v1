package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Group Header - Common header information for ISO 20022 messages.
 * Used in pain, pacs, camt, and other message types.
 *
 * Contains identification, creation timestamp, and summary information
 * about the message content.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupHeader {

    /**
     * Message Identification - Unique identifier assigned by the instructing party.
     * Max 35 characters.
     */
    @NotBlank(message = "Message ID is required")
    @Size(max = 35, message = "Message ID must not exceed 35 characters")
    private String messageId;

    /**
     * Creation Date Time - Date and time when the message was created.
     */
    @NotNull(message = "Creation date time is required")
    private ZonedDateTime creationDateTime;

    /**
     * Number of Transactions - Total number of individual transactions in the message.
     */
    private Integer numberOfTransactions;

    /**
     * Control Sum - Total of all individual amounts in the message.
     * Used for integrity checking.
     */
    private BigDecimal controlSum;

    /**
     * Instructing Agent - Agent (bank) that instructs the next party in the chain.
     */
    private AgentIdentification instructingAgent;

    /**
     * Instructed Agent - Agent (bank) that is instructed by the previous party.
     */
    private AgentIdentification instructedAgent;

    /**
     * Initiating Party - Party that initiates the message.
     * Typically the customer in pain.* messages.
     */
    private PartyIdentification initiatingParty;

    /**
     * Settlement Information - Information about how the payment will be settled.
     */
    private SettlementInformation settlementInformation;

    /**
     * Interbank Settlement Date - Date when the interbank settlement should occur.
     * For FedNow, this is always T+0 (same day).
     */
    private java.time.LocalDate interbankSettlementDate;

    /**
     * Total Interbank Settlement Amount - Total amount to be settled between banks.
     */
    private Amount totalInterbankSettlementAmount;
}
