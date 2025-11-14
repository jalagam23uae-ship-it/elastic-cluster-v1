package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contact Details - Communication information for a party.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactDetails {

    /**
     * Name Prefix - Title or salutation (Mr., Mrs., Dr., etc.).
     * Max 35 characters.
     */
    @Size(max = 35, message = "Name prefix must not exceed 35 characters")
    private String namePrefix;

    /**
     * Name - Contact person's name.
     * Max 140 characters.
     */
    @Size(max = 140, message = "Name must not exceed 140 characters")
    private String name;

    /**
     * Phone Number - Telephone number.
     * Max 20 characters.
     */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    /**
     * Mobile Number - Mobile/cell phone number.
     * Max 20 characters.
     */
    @Size(max = 20, message = "Mobile number must not exceed 20 characters")
    private String mobileNumber;

    /**
     * Fax Number - Fax number.
     * Max 20 characters.
     */
    @Size(max = 20, message = "Fax number must not exceed 20 characters")
    private String faxNumber;

    /**
     * Email Address - Email address.
     * Max 256 characters.
     */
    @Email(message = "Email must be valid")
    @Size(max = 256, message = "Email must not exceed 256 characters")
    private String emailAddress;

    /**
     * Other - Other contact method description.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Other must not exceed 35 characters")
    private String other;
}
