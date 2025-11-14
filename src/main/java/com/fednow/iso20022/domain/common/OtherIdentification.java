package com.fednow.iso20022/domain/common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Other Identification - Alternative identification scheme.
 *
 * Used for identifications that don't fit standard schemes like IBAN or BIC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtherIdentification {

    /**
     * Identification - The actual identifier value.
     * Max 35 characters.
     */
    @NotBlank(message = "Identification is required")
    @Size(max = 35, message = "Identification must not exceed 35 characters")
    private String identification;

    /**
     * Scheme Name - Name or code of the identification scheme.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Scheme name must not exceed 35 characters")
    private String schemeName;

    /**
     * Issuer - Party that issued the identifier.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Issuer must not exceed 35 characters")
    private String issuer;
}
