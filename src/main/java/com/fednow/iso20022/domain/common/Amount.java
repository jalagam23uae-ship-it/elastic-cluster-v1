package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Amount - Monetary amount with currency code.
 *
 * Represents a monetary value with its currency. For FedNow, the currency
 * is always USD and amounts must be between $0.01 and $500,000.00.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Amount {

    /**
     * The monetary value.
     * For FedNow: Min = 0.01, Max = 500,000.00
     */
    @NotNull(message = "Amount value is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal value;

    /**
     * Currency code using ISO 4217 alphabetic codes.
     * For FedNow, this must be "USD".
     * Exactly 3 characters.
     */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Builder.Default
    private String currency = "USD";

    /**
     * Convenience method to create a USD amount.
     *
     * @param value the amount value
     * @return Amount object with USD currency
     */
    public static Amount usd(BigDecimal value) {
        return Amount.builder()
                .value(value)
                .currency("USD")
                .build();
    }

    /**
     * Convenience method to create a USD amount from a double.
     *
     * @param value the amount value
     * @return Amount object with USD currency
     */
    public static Amount usd(double value) {
        return Amount.builder()
                .value(BigDecimal.valueOf(value))
                .currency("USD")
                .build();
    }

    /**
     * Convenience method to create a USD amount from a String.
     *
     * @param value the amount value as string
     * @return Amount object with USD currency
     */
    public static Amount usd(String value) {
        return Amount.builder()
                .value(new BigDecimal(value))
                .currency("USD")
                .build();
    }
}
