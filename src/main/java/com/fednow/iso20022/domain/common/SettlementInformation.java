package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Settlement Information - Describes how the payment will be settled.
 *
 * For FedNow payments:
 * - Settlement Method: INDA (Instructed Agent)
 * - Clearing System: FDW (FedNow)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInformation {

    /**
     * Settlement Method - Method used to settle the payment.
     *
     * Common codes:
     * - INDA: Instructed Agent (FedNow uses this)
     * - INGA: Instructing Agent
     * - COVE: Cover Method
     * - CLRG: Clearing System
     *
     * Max 4 characters.
     */
    @NotBlank(message = "Settlement method is required")
    @Size(max = 4, message = "Settlement method must not exceed 4 characters")
    @Builder.Default
    private String settlementMethod = "INDA";

    /**
     * Clearing System - Identifies the clearing and settlement system.
     *
     * For FedNow: "FDW"
     * Max 5 characters.
     */
    @Size(max = 5, message = "Clearing system must not exceed 5 characters")
    @Builder.Default
    private String clearingSystem = "FDW";

    /**
     * Settlement Account - Account used for settlement.
     */
    private AccountIdentification settlementAccount;

    /**
     * Convenience method to create FedNow settlement information.
     *
     * @return SettlementInformation configured for FedNow
     */
    public static SettlementInformation fednow() {
        return SettlementInformation.builder()
                .settlementMethod("INDA")
                .clearingSystem("FDW")
                .build();
    }
}
