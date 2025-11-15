package com.fednow.iso20022.entity.enums;

/**
 * ISO 20022 message types supported by the system
 */
public enum MessageType {
    // Pain (Payment Initiation) messages
    PAIN_001,  // CustomerCreditTransferInitiation
    PAIN_002,  // CustomerPaymentStatusReport
    PAIN_007,  // CustomerPaymentReversal
    PAIN_008,  // CustomerDirectDebitInitiation
    PAIN_009,  // MandateInitiationRequest
    PAIN_013,  // CreditorPaymentActivationRequest
    PAIN_014,  // CreditorPaymentActivationRequestStatusReport
    
    // Pacs (Payment Clearing and Settlement) messages
    PACS_002,  // FIToFIPaymentStatusReport
    PACS_003,  // FIToFICustomerDirectDebit
    PACS_004,  // PaymentReturn
    PACS_007,  // FIToFIPaymentReversal
    PACS_008,  // FIToFICustomerCreditTransfer
    PACS_009,  // FinancialInstitutionCreditTransfer
    PACS_028,  // FIToFIPaymentStatusRequest
    
    // Camt (Cash Management) messages
    CAMT_028,  // AdditionalPaymentInformation
    CAMT_029,  // ResolutionOfInvestigation
    CAMT_050,  // LiquidityCreditTransfer
    CAMT_052,  // BankToCustomerAccountReport
    CAMT_053,  // BankToCustomerStatement
    CAMT_054,  // BankToCustomerDebitCreditNotification
    CAMT_056,  // FIToFIPaymentCancellationRequest
    CAMT_057,  // NotificationToReceive
    CAMT_058,  // NotificationToReceiveCancellationAdvice
    
    // Acmt (Account Management) messages
    ACMT_007,  // AccountOpeningInstruction
    ACMT_023,  // IdentificationModificationAdvice
    ACMT_024,  // IdentificationVerificationRequest
    
    // Admi (Administration) messages
    ADMI_002,  // SystemEventNotification
    ADMI_007   // ReceiptAcknowledgement
}
