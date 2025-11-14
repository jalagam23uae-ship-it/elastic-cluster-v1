package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.*;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain001;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomerCreditTransferToPacs008Converter - CRITICAL CONVERTER
 *
 * Converts pain.001 (Customer Credit Transfer Initiation) to pacs.008 (FI to FI Customer Credit Transfer).
 *
 * This is the MOST IMPORTANT converter for payment initiation:
 * - Customer initiates payment (pain.001)
 * - Bank converts to interbank format (pacs.008)
 * - Send to FedNow for settlement
 *
 * Key Transformations:
 * 1. Generate UETR (UUID v4) for end-to-end tracking
 * 2. Add bank identification (instructing agent = bank's BIC/routing)
 * 3. Add settlement information (INDA settlement method, FDW clearing system)
 * 4. Set interbank settlement date (T+0 for FedNow)
 * 5. Generate transaction ID
 * 6. Preserve End-to-End ID for tracking
 *
 * Validation (simplified for now):
 * - Currency must be USD
 * - Amount within FedNow limits ($0.01 - $500,000)
 * - Execution date must be current date (T+0)
 * - Required fields present
 */
@Slf4j
@Component
public class CustomerCreditTransferToPacs008Converter
        extends AbstractMessageConverter<Pain001, Pacs008> {

    public CustomerCreditTransferToPacs008Converter() {
        super(Pain001.class, Pacs008.class, "CustomerCreditTransferToPacs008Converter");
    }

    @Override
    protected Mono<Pacs008> doConvert(Pain001 source, ConverterContext context) {
        logStep("Starting pain.001 → pacs.008 conversion");

        // Generate IDs: message ID, UETR, transaction ID
        enrichContextWithIds(context, "BANK-FDW", true, true);

        return Mono.fromCallable(() -> {
            // Build pacs.008
            Pacs008 pacs008 = Pacs008.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .creditTransferTransactionInformation(
                            buildCreditTransferTransactions(source, context))
                    .build();

            logStep("Successfully created pacs.008 with UETR: " + context.getGeneratedUetr());
            return pacs008;
        });
    }

    /**
     * Builds the group header for pacs.008.
     *
     * @param source original pain.001
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pain001 source, ConverterContext context) {
        // Calculate totals from all payment information blocks
        int totalTransactions = source.getPaymentInformation().stream()
                .mapToInt(pi -> pi.getCreditTransferTransactionInformation().size())
                .sum();

        java.math.BigDecimal totalAmount = source.getPaymentInformation().stream()
                .flatMap(pi -> pi.getCreditTransferTransactionInformation().stream())
                .map(Pain001.CreditTransferTransactionInformation::getAmount)
                .map(Amount::getValue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return GroupHeader.builder()
                // Bank assigns new message ID
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .numberOfTransactions(totalTransactions)
                .controlSum(totalAmount)
                // Settlement information for FedNow
                .settlementInformation(SettlementInformation.fednow())
                .interbankSettlementDate(context.getCurrentDate())
                .totalInterbankSettlementAmount(Amount.usd(totalAmount))
                // Bank is the instructing agent
                .instructingAgent(context.getBankContext().getBankIdentification())
                // Get instructed agent from first transaction (creditor agent)
                .instructedAgent(getInstructedAgent(source))
                .build();
    }

    /**
     * Gets the instructed agent (beneficiary bank) from the first transaction.
     *
     * @param source pain.001
     * @return instructed agent
     */
    private AgentIdentification getInstructedAgent(Pain001 source) {
        return source.getPaymentInformation().stream()
                .flatMap(pi -> pi.getCreditTransferTransactionInformation().stream())
                .findFirst()
                .map(Pain001.CreditTransferTransactionInformation::getCreditorAgent)
                .orElse(null);
    }

    /**
     * Builds credit transfer transactions for pacs.008.
     *
     * @param source original pain.001
     * @param context converter context
     * @return list of credit transfer transactions
     */
    private List<Pacs008.CreditTransferTransactionInformation> buildCreditTransferTransactions(
            Pain001 source, ConverterContext context) {

        List<Pacs008.CreditTransferTransactionInformation> transactions = new ArrayList<>();

        for (Pain001.PaymentInformation pmtInf : source.getPaymentInformation()) {
            for (Pain001.CreditTransferTransactionInformation custTxn :
                    pmtInf.getCreditTransferTransactionInformation()) {

                Pacs008.CreditTransferTransactionInformation interbankTxn =
                        buildCreditTransferTransaction(pmtInf, custTxn, context);
                transactions.add(interbankTxn);
            }
        }

        return transactions;
    }

    /**
     * Builds a single credit transfer transaction.
     *
     * @param pmtInf payment information block from pain.001
     * @param custTxn customer transaction from pain.001
     * @param context converter context
     * @return interbank transaction for pacs.008
     */
    private Pacs008.CreditTransferTransactionInformation buildCreditTransferTransaction(
            Pain001.PaymentInformation pmtInf,
            Pain001.CreditTransferTransactionInformation custTxn,
            ConverterContext context) {

        return Pacs008.CreditTransferTransactionInformation.builder()
                // Payment identification
                .paymentIdentification(PaymentIdentification.builder()
                        // Bank assigns instruction ID
                        .instructionId(idGenerator.generateInstructionId())
                        // PRESERVE End-to-End ID from customer
                        .endToEndId(custTxn.getPaymentIdentification().getEndToEndId())
                        // Bank assigns transaction ID
                        .transactionId(context.getGeneratedTransactionId())
                        // Bank generates UETR
                        .uetr(context.getGeneratedUetr())
                        .build())
                // Payment type information
                .paymentTypeInformation(buildPaymentTypeInformation(custTxn))
                // Interbank settlement amount (same as instructed amount for USD)
                .interbankSettlementAmount(custTxn.getAmount())
                .interbankSettlementDate(context.getCurrentDate())
                // Settlement priority
                .settlementPriority("NORM") // Normal priority for FedNow
                // Instructed amount
                .instructedAmount(custTxn.getAmount())
                // Charge bearer (default SHAR for FedNow)
                .chargeBearer(custTxn.getChargeBearer() != null
                        ? custTxn.getChargeBearer()
                        : "SHAR")
                // Instructing agent (bank sending the payment)
                .instructingAgent(context.getBankContext().getBankIdentification())
                // Instructed agent (beneficiary bank)
                .instructedAgent(custTxn.getCreditorAgent())
                // Debtor information
                .debtor(pmtInf.getDebtor())
                .debtorAccount(pmtInf.getDebtorAccount())
                .debtorAgent(pmtInf.getDebtorAgent())
                // Ultimate debtor (if specified)
                .ultimateDebtor(pmtInf.getUltimateDebtor())
                // Creditor information
                .creditor(custTxn.getCreditor())
                .creditorAccount(custTxn.getCreditorAccount())
                .creditorAgent(custTxn.getCreditorAgent())
                // Ultimate creditor (if specified)
                .ultimateCreditor(custTxn.getUltimateCreditor())
                // Purpose
                .purpose(custTxn.getPurpose() != null
                        ? Pacs008.Purpose.builder()
                        .code(custTxn.getPurpose().getCode())
                        .proprietary(custTxn.getPurpose().getProprietary())
                        .build()
                        : null)
                // Remittance information
                .remittanceInformation(custTxn.getRemittanceInformation())
                // Regulatory reporting (if any)
                .regulatoryReporting(buildRegulatoryReporting(custTxn))
                .build();
    }

    /**
     * Builds payment type information for pacs.008.
     *
     * @param custTxn customer transaction
     * @return payment type information
     */
    private Pacs008.PaymentTypeInformation buildPaymentTypeInformation(
            Pain001.CreditTransferTransactionInformation custTxn) {

        if (custTxn.getPaymentTypeInformation() == null) {
            // Default payment type for FedNow
            return Pacs008.PaymentTypeInformation.builder()
                    .instructionPriority("NORM")
                    .clearingChannel("RTGS") // Real-time gross settlement
                    .serviceLevel("SDVA") // Same day value
                    .build();
        }

        return Pacs008.PaymentTypeInformation.builder()
                .instructionPriority(custTxn.getPaymentTypeInformation()
                        .getInstructionPriority() != null
                        ? custTxn.getPaymentTypeInformation().getInstructionPriority()
                        : "NORM")
                .clearingChannel("RTGS") // FedNow always uses RTGS
                .serviceLevel(custTxn.getPaymentTypeInformation().getServiceLevel() != null
                        ? custTxn.getPaymentTypeInformation().getServiceLevel()
                        : "SDVA")
                .localInstrument(custTxn.getPaymentTypeInformation().getLocalInstrument())
                .categoryPurpose(custTxn.getPaymentTypeInformation().getCategoryPurpose())
                .build();
    }

    /**
     * Builds regulatory reporting information.
     *
     * @param custTxn customer transaction
     * @return regulatory reporting list (if any)
     */
    private List<Pacs008.RegulatoryReporting> buildRegulatoryReporting(
            Pain001.CreditTransferTransactionInformation custTxn) {

        if (custTxn.getRegulatoryReporting() == null ||
                custTxn.getRegulatoryReporting().isEmpty()) {
            return null;
        }

        List<Pacs008.RegulatoryReporting> reporting = new ArrayList<>();

        for (Pain001.RegulatoryReporting custRep : custTxn.getRegulatoryReporting()) {
            Pacs008.RegulatoryReporting interbankRep = Pacs008.RegulatoryReporting.builder()
                    .debitCreditReportingIndicator(custRep.getDebitCreditReportingIndicator())
                    .authority(custRep.getAuthority() != null
                            ? Pacs008.RegulatoryAuthority.builder()
                            .name(custRep.getAuthority().getName())
                            .country(custRep.getAuthority().getCountry())
                            .build()
                            : null)
                    .details(buildRegulatoryDetails(custRep))
                    .build();

            reporting.add(interbankRep);
        }

        return reporting;
    }

    /**
     * Builds regulatory reporting details.
     *
     * @param custRep customer regulatory reporting
     * @return regulatory details list
     */
    private List<Pacs008.StructuredRegulatoryReporting> buildRegulatoryDetails(
            Pain001.RegulatoryReporting custRep) {

        if (custRep.getDetails() == null || custRep.getDetails().isEmpty()) {
            return null;
        }

        List<Pacs008.StructuredRegulatoryReporting> details = new ArrayList<>();

        for (Pain001.StructuredRegulatoryReporting custDetail : custRep.getDetails()) {
            Pacs008.StructuredRegulatoryReporting detail =
                    Pacs008.StructuredRegulatoryReporting.builder()
                            .type(custDetail.getType())
                            .information(custDetail.getInformation())
                            .amount(custDetail.getAmount())
                            .country(custDetail.getCountry())
                            .build();

            details.add(detail);
        }

        return details;
    }

    @Override
    public Mono<Void> validate(Pain001 source, ConverterContext context) {
        return super.validate(source, context)
                .then(Mono.defer(() -> {
                    // Basic validation (skip detailed validation for now)

                    // Check payment information exists
                    if (source.getPaymentInformation() == null ||
                            source.getPaymentInformation().isEmpty()) {
                        return conversionError(
                                "Pain.001 must contain at least one payment information block",
                                "NO_PAYMENT_INFO");
                    }

                    // Check each payment information has transactions
                    for (Pain001.PaymentInformation pmtInf : source.getPaymentInformation()) {
                        if (pmtInf.getCreditTransferTransactionInformation() == null ||
                                pmtInf.getCreditTransferTransactionInformation().isEmpty()) {
                            return conversionError(
                                    "Payment information must contain at least one transaction",
                                    "NO_TRANSACTIONS");
                        }
                    }

                    // All checks passed
                    return Mono.empty();
                }));
    }
}
