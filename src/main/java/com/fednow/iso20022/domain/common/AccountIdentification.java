package com.fednow.iso20022.domain.common;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account Identification - Identifies a bank account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountIdentification {

    /**
     * IBAN - International Bank Account Number.
     * Max 34 characters.
     */
    @Size(max = 34, message = "IBAN must not exceed 34 characters")
    private String iban;

    /**
     * Other Identification - Used for non-IBAN accounts.
     * In the US, this is typically the account number.
     */
    private OtherIdentification otherIdentification;

    /**
     * Account Name - Name of the account.
     * Max 70 characters.
     */
    @Size(max = 70, message = "Account name must not exceed 70 characters")
    private String accountName;

    /**
     * Account Type - Type of account.
     * CACC: Current/Checking account
     * SVGS: Savings account
     * CASH: Cash account
     * etc.
     */
    @Size(max = 4, message = "Account type must not exceed 4 characters")
    private String accountType;

    /**
     * Currency - Currency of the account.
     * ISO 4217 alphabetic code (3 characters).
     */
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    private String currency;

    /**
     * Convenience method to create account identification with account number.
     *
     * @param accountNumber the account number
     * @return AccountIdentification with account number set
     */
    public static AccountIdentification withAccountNumber(String accountNumber) {
        return AccountIdentification.builder()
                .otherIdentification(OtherIdentification.builder()
                        .identification(accountNumber)
                        .build())
                .build();
    }

    /**
     * Convenience method to create account identification with IBAN.
     *
     * @param iban the IBAN
     * @return AccountIdentification with IBAN set
     */
    public static AccountIdentification withIban(String iban) {
        return AccountIdentification.builder()
                .iban(iban)
                .build();
    }
}
