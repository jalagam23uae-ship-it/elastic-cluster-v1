package com.fednow.iso20022.entity.enums;

/**
 * Account status enumeration
 */
public enum AccountStatus {
    ACTIVE,      // Account is active and can process transactions
    BLOCKED,     // Account is blocked (e.g., due to fraud, legal hold)
    CLOSED,      // Account has been closed
    SUSPENDED,   // Account is temporarily suspended
    DORMANT      // Account is dormant (inactive for extended period)
}
