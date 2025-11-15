package com.fednow.iso20022.entity.enums;

/**
 * Request for Payment status
 */
public enum RfpStatus {
    REQUESTED,   // RFP has been requested
    ACCEPTED,    // RFP has been accepted
    REJECTED,    // RFP has been rejected
    CANCELLED,   // RFP has been cancelled
    EXPIRED      // RFP has expired
}
