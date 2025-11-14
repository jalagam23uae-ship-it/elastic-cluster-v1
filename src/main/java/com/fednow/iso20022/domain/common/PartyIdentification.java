package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Party Identification - Identifies a party (person or organization) in the payment chain.
 *
 * Used for debtor, creditor, initiating party, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyIdentification {

    /**
     * Name of the party.
     * Max 140 characters.
     */
    @Size(max = 140, message = "Name must not exceed 140 characters")
    private String name;

    /**
     * Postal address of the party.
     */
    private Address address;

    /**
     * Identification of the party - Can be organization ID, personal ID, etc.
     */
    private OrganisationIdentification organisationIdentification;

    /**
     * Personal identification - For individual persons.
     */
    private PersonIdentification personIdentification;

    /**
     * Country of residence.
     * ISO 3166-1 alpha-2 country code (2 characters).
     */
    @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
    private String countryOfResidence;

    /**
     * Contact details for the party.
     */
    private ContactDetails contactDetails;

    /**
     * Convenience method to create a party with just a name.
     *
     * @param name the party name
     * @return PartyIdentification with name set
     */
    public static PartyIdentification withName(String name) {
        return PartyIdentification.builder()
                .name(name)
                .build();
    }
}
