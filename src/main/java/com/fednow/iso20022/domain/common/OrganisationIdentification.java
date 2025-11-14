package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Organisation Identification - Identification information for a company/organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationIdentification {

    /**
     * Business Identifier Code - BIC or SWIFT code for financial institutions.
     * Max 11 characters.
     */
    @Size(max = 11, message = "BIC must not exceed 11 characters")
    private String bic;

    /**
     * Legal Entity Identifier (LEI) - Global unique identifier for legal entities.
     * 20 characters.
     */
    @Size(min = 20, max = 20, message = "LEI must be exactly 20 characters")
    private String lei;

    /**
     * Other Identification - Alternative identification (e.g., Tax ID, Registration number).
     */
    private OtherIdentification otherIdentification;
}
