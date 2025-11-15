package com.fednow.iso20022.entity.enums;

/**
 * ISO 20022 payment status codes
 */
public enum PaymentStatusCode {
    ACCP,  // AcceptedCustomerProfile
    ACSC,  // AcceptedSettlementCompleted
    ACSP,  // AcceptedSettlementInProcess
    ACTC,  // AcceptedTechnicalValidation
    ACWC,  // AcceptedWithChange
    PART,  // PartiallyAccepted
    PDNG,  // Pending
    RCVD,  // Received
    RJCT,  // Rejected
    CANC   // Cancelled
}
