package com.fednow.iso20022.entity.enums;

/**
 * Status of a message in its lifecycle
 */
public enum MessageStatus {
    RECEIVED,      // Message received from external system
    VALIDATED,     // Message passed validation
    CONVERTED,     // Message converted to another format
    SENT,          // Message sent to external system
    ACKNOWLEDGED,  // Message acknowledged by recipient
    ACCEPTED,      // Message accepted for processing
    REJECTED,      // Message rejected
    PENDING,       // Message pending further action
    FAILED,        // Message processing failed
    ERROR          // Error occurred during processing
}
