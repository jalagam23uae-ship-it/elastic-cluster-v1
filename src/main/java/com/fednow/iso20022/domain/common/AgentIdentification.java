package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent Identification - Identifies a financial institution (bank) in the payment chain.
 *
 * Used for debtor agent, creditor agent, instructing agent, instructed agent, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentIdentification {

    /**
     * BIC (Bank Identifier Code) - SWIFT code identifying the financial institution.
     * Format: 8 or 11 characters (AAAA BB CC DDD)
     * - AAAA: Institution code (4 chars)
     * - BB: Country code (2 chars)
     * - CC: Location code (2 chars)
     * - DDD: Branch code (3 chars, optional)
     *
     * Example: BANKUS33XXX
     */
    @Size(min = 8, max = 11, message = "BIC must be 8 or 11 characters")
    private String bicFi;

    /**
     * Clearing System Member Identification - Identifies the member within a clearing system.
     * For FedNow in the US, this is typically the ABA routing number (9 digits).
     *
     * Example: 026009593
     */
    private ClearingSystemMemberIdentification clearingSystemMemberId;

    /**
     * Name of the financial institution.
     * Max 140 characters.
     */
    @Size(max = 140, message = "Name must not exceed 140 characters")
    private String name;

    /**
     * Postal address of the financial institution.
     */
    private Address address;

    /**
     * Other identification - Proprietary or other identification schemes.
     */
    private OtherIdentification otherIdentification;

    /**
     * Convenience method to create agent with BIC only.
     *
     * @param bicFi the BIC/SWIFT code
     * @return AgentIdentification with BIC set
     */
    public static AgentIdentification withBic(String bicFi) {
        return AgentIdentification.builder()
                .bicFi(bicFi)
                .build();
    }

    /**
     * Convenience method to create agent with routing number only.
     *
     * @param routingNumber the US ABA routing number
     * @return AgentIdentification with routing number set
     */
    public static AgentIdentification withRoutingNumber(String routingNumber) {
        return AgentIdentification.builder()
                .clearingSystemMemberId(ClearingSystemMemberIdentification.builder()
                        .memberId(routingNumber)
                        .build())
                .build();
    }

    /**
     * Convenience method to create agent with both BIC and routing number.
     *
     * @param bicFi the BIC/SWIFT code
     * @param routingNumber the US ABA routing number
     * @return AgentIdentification with both BIC and routing number set
     */
    public static AgentIdentification withBicAndRouting(String bicFi, String routingNumber) {
        return AgentIdentification.builder()
                .bicFi(bicFi)
                .clearingSystemMemberId(ClearingSystemMemberIdentification.builder()
                        .memberId(routingNumber)
                        .build())
                .build();
    }
}
