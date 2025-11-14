package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Address - Postal address information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /**
     * Address Type - Code specifying the type of address.
     * ADDR: Postal address
     * PBOX: Post office box
     * HOME: Home address
     * BIZZ: Business address
     * MLTO: Mail to address
     * DLVY: Delivery to address
     */
    @Size(max = 4, message = "Address type must not exceed 4 characters")
    private String addressType;

    /**
     * Department - Identification of a department.
     * Max 70 characters.
     */
    @Size(max = 70, message = "Department must not exceed 70 characters")
    private String department;

    /**
     * Sub Department - Identification of a sub-department.
     * Max 70 characters.
     */
    @Size(max = 70, message = "Sub department must not exceed 70 characters")
    private String subDepartment;

    /**
     * Street Name - Name of the street or thoroughfare.
     * Max 70 characters.
     */
    @Size(max = 70, message = "Street name must not exceed 70 characters")
    private String streetName;

    /**
     * Building Number - Number that identifies the building.
     * Max 16 characters.
     */
    @Size(max = 16, message = "Building number must not exceed 16 characters")
    private String buildingNumber;

    /**
     * Post Code - Postal code for the address.
     * Max 16 characters.
     */
    @Size(max = 16, message = "Post code must not exceed 16 characters")
    private String postCode;

    /**
     * Town Name - Name of the town or city.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Town name must not exceed 35 characters")
    private String townName;

    /**
     * Country Sub Division - State, province, or region.
     * Max 35 characters.
     */
    @Size(max = 35, message = "Country sub division must not exceed 35 characters")
    private String countrySubDivision;

    /**
     * Country - Country code using ISO 3166-1 alpha-2 (2 characters).
     * Example: "US" for United States
     */
    @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
    private String country;

    /**
     * Address Line - Multiple free-form address lines (max 7 lines).
     * Each line max 70 characters.
     */
    private List<String> addressLine;
}
