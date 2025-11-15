package com.fednow.iso20022.entity.enums;

/**
 * Direct debit mandate status
 */
public enum MandateStatus {
    ACTIVE,      // Mandate is active and can be used
    SUSPENDED,   // Mandate is temporarily suspended
    REVOKED,     // Mandate has been revoked
    EXPIRED,     // Mandate has expired
    PENDING      // Mandate is pending activation
}
