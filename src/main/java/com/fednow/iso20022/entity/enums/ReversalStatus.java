package com.fednow.iso20022.entity.enums;

/**
 * Payment reversal status
 */
public enum ReversalStatus {
    REQUESTED,   // Reversal requested
    ACCEPTED,    // Reversal accepted
    SETTLED,     // Reversal settled
    REJECTED,    // Reversal rejected (e.g., outside 15-second window)
    EXPIRED      // Reversal request expired
}
