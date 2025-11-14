package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Clearing System Member Identification - Identifies a member within a clearing system.
 *
 * For FedNow in the US, this is typically the ABA routing number (9 digits).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClearingSystemMemberIdentification {

    /**
     * Clearing System Identification - Identifies the clearing system.
     * For FedNow: "FDW"
     * Max 5 characters.
     */
    @Size(max = 5, message = "Clearing system ID must not exceed 5 characters")
    private String clearingSystemId;

    /**
     * Member Identification - The member's identifier within the clearing system.
     * For US banks: 9-digit ABA routing number (e.g., "026009593")
     * Max 35 characters.
     */
    @NotBlank(message = "Member ID is required")
    @Size(max = 35, message = "Member ID must not exceed 35 characters")
    private String memberId;

    /**
     * Convenience method to create a FedNow member identification.
     *
     * @param routingNumber the US ABA routing number
     * @return ClearingSystemMemberIdentification for FedNow
     */
    public static ClearingSystemMemberIdentification fednow(String routingNumber) {
        return ClearingSystemMemberIdentification.builder()
                .clearingSystemId("FDW")
                .memberId(routingNumber)
                .build();
    }
}
