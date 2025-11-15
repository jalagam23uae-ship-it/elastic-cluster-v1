package com.fednow.iso20022.entity.enums;

/**
 * Payment investigation status
 */
public enum InvestigationStatus {
    OPEN,        // Investigation is open
    PENDING,     // Investigation is pending response
    RESOLVED,    // Investigation has been resolved
    CLOSED,      // Investigation is closed
    ESCALATED    // Investigation has been escalated
}
