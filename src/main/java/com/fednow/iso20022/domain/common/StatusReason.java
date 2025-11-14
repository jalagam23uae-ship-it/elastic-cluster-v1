package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status Reason - Provides reason for a specific payment status.
 *
 * Used in status reports (pacs.002, pain.002) to explain acceptance, rejection, or pending status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusReason {

    /**
     * Originator - Party that originated the status.
     */
    private PartyIdentification originator;

    /**
     * Reason Code - Standardized code explaining the status.
     *
     * Common rejection codes:
     * - AC01: Incorrect Account Number
     * - AC04: Closed Account
     * - AC06: Blocked Account
     * - AG01: Transaction Forbidden (OFAC, sanctions)
     * - AM04: Insufficient Funds
     * - AM09: Wrong Amount
     * - FF01: Invalid File Format
     * - MS03: Not Specified Reason
     * - RC01: Bank Identifier Incorrect
     * - RR01: Missing Debtor Account/Identification
     * - RR03: Missing Creditor Name/Address
     *
     * Max 4 characters.
     */
    @Size(max = 4, message = "Reason code must not exceed 4 characters")
    private String reasonCode;

    /**
     * Additional Information - Free-text explanation of the reason.
     * Provides human-readable details beyond the code.
     * Max 105 characters per line.
     */
    private java.util.List<String> additionalInformation;

    /**
     * Convenience method to create status reason with code and message.
     *
     * @param reasonCode the ISO 20022 reason code
     * @param message explanatory message
     * @return StatusReason with code and message
     */
    public static StatusReason of(String reasonCode, String message) {
        return StatusReason.builder()
                .reasonCode(reasonCode)
                .additionalInformation(java.util.List.of(message))
                .build();
    }

    /**
     * Common predefined status reasons.
     */
    public static class CommonReasons {
        public static final StatusReason INCORRECT_ACCOUNT = of("AC01", "Incorrect account number provided");
        public static final StatusReason CLOSED_ACCOUNT = of("AC04", "Account closed");
        public static final StatusReason BLOCKED_ACCOUNT = of("AC06", "Account blocked");
        public static final StatusReason TRANSACTION_FORBIDDEN = of("AG01", "Transaction forbidden");
        public static final StatusReason INSUFFICIENT_FUNDS = of("AM04", "Insufficient funds");
        public static final StatusReason WRONG_AMOUNT = of("AM09", "Wrong amount");
        public static final StatusReason INVALID_FORMAT = of("FF01", "Invalid file format");
        public static final StatusReason NOT_SPECIFIED = of("MS03", "Not specified reason");
        public static final StatusReason BANK_IDENTIFIER_INCORRECT = of("RC01", "Bank identifier incorrect");
    }
}
