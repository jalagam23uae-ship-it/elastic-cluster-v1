package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Person Identification - Identification information for an individual person.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonIdentification {

    /**
     * Date of Birth.
     */
    private LocalDate dateOfBirth;

    /**
     * Place of Birth.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Place of birth must not exceed 35 characters")
    private String placeOfBirth;

    /**
     * Other Identification - Alternative identification (e.g., Passport, Driver's License, SSN).
     */
    private OtherIdentification otherIdentification;
}
