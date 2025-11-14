package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Remittance Information - Information about the purpose of payment.
 *
 * Provides details about what the payment is for (invoice number, description, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemittanceInformation {

    /**
     * Unstructured - Free-form text describing the payment purpose.
     * Each line max 140 characters.
     * Can have multiple lines.
     */
    private List<String> unstructured;

    /**
     * Structured - Structured remittance information with specific fields.
     * More detailed than unstructured.
     */
    private StructuredRemittanceInformation structured;

    /**
     * Convenience method to create remittance info with single unstructured text.
     *
     * @param text the remittance text
     * @return RemittanceInformation with unstructured text
     */
    public static RemittanceInformation withText(String text) {
        return RemittanceInformation.builder()
                .unstructured(List.of(text))
                .build();
    }

    /**
     * Structured Remittance Information - Detailed payment purpose information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredRemittanceInformation {

        /**
         * Referred Document Information - Information about documents related to payment.
         */
        private List<ReferredDocumentInformation> referredDocumentInformation;

        /**
         * Additional Remittance Information - Additional free-text information.
         * Max 140 characters per line.
         */
        private List<String> additionalRemittanceInformation;
    }

    /**
     * Referred Document Information - Information about a specific document (invoice, etc.).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferredDocumentInformation {

        /**
         * Type - Type of document (e.g., CINV for commercial invoice).
         * Max 4 characters.
         */
        @Size(max = 4, message = "Type must not exceed 4 characters")
        private String type;

        /**
         * Number - Document number (e.g., invoice number).
         * Max 35 characters.
         */
        @Size(max = 35, message = "Number must not exceed 35 characters")
        private String number;

        /**
         * Related Date - Date related to the document (e.g., invoice date).
         */
        private java.time.LocalDate relatedDate;
    }
}
