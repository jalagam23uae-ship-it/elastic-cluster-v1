package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.common.SettlementInformation;
import com.fednow.iso20022.domain.pacs.Pacs003;
import com.fednow.iso20022.domain.pain.Pain008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pain008ToPacs003Converter - Customer Direct Debit to Interbank Direct Debit
 *
 * Converts pain.008 (Customer Direct Debit Initiation) to pacs.003 (FI to FI Customer Direct Debit).
 *
 * This converter transforms a customer-initiated direct debit request into an interbank
 * direct debit message that can be sent through FedNow to collect funds from the debtor's account.
 *
 * Key Transformations:
 * 1. Generate new interbank message ID
 * 2. Add FedNow settlement information (INDA, FDW)
 * 3. Enrich agent details with full bank information
 * 4. Generate UETR for tracking
 * 5. Validate and preserve mandate information
 * 6. Set T+0 settlement date
 *
 * Difference from pain.001 → pacs.008:
 * - pain.001/pacs.008: PUSH payment (debtor initiates)
 * - pain.008/pacs.003: PULL payment (creditor initiates with authorization)
 *
 * Message Flow:
 * Customer (Creditor) → pain.008 → Creditor Bank → pacs.003 → FedNow → Debtor Bank
 */
@Slf4j
@Component
public class Pain008ToPacs003Converter extends AbstractMessageConverter<Pain008, Pacs003> {

    public Pain008ToPacs003Converter() {
        super(Pain008.class, Pacs003.class, "Pain008ToPacs003Converter");
    }

    @Override
    protected Mono<Pacs003> doConvert(Pain008 source, ConverterContext context) {
        logStep("Starting pain.008 → pacs.003 conversion (customer direct debit → interbank direct debit)");

        // Generate message ID and UETR
        enrichContextWithIds(context, "DD", true, false);

        return Mono.fromCallable(() -> {
            // Validate pain.008
            validatePain008(source);

            // Build pacs.003
            Pacs003 pacs003 = Pacs003.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .directDebitTransactionInformation(buildDirectDebitTransactions(source, context))
                    .build();

            logStep("Successfully created pacs.003 interbank direct debit");
            return pacs003;
        });
    }

    /**
     * Validates pain.008 input.
     */
    private void validatePain008(Pain008 source) {
        if (source.getGroupHeader() == null) {
            throw new IllegalArgumentException("Group header is required");
        }

        if (source.getPaymentInformation() == null || source.getPaymentInformation().isEmpty()) {
            throw new IllegalArgumentException("Payment information is required");
        }

        // Validate mandate information exists
        Pain008.PaymentInformation pmtInf = source.getPaymentInformation().get(0);
        if (pmtInf.getDirectDebitTransactionInformation() != null) {
            for (Pain008.DirectDebitTransactionInformation txn :
                    pmtInf.getDirectDebitTransactionInformation()) {
                if (txn.getDirectDebitTransaction() == null ||
                    txn.getDirectDebitTransaction().getMandateRelatedInformation() == null) {
                    throw new IllegalArgumentException(
                            "Mandate information is required for direct debit");
                }
            }
        }

        logStep("pain.008 validation passed");
    }

    /**
     * Builds group header for pacs.003.
     */
    private GroupHeader buildGroupHeader(Pain008 source, ConverterContext context) {
        Pain008.PaymentInformation firstPmt = source.getPaymentInformation().get(0);

        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Creditor bank is instructing agent
                .instructingAgent(firstPmt.getCreditorAgent())
                // Debtor bank is instructed agent (will be enriched from first transaction)
                .instructedAgent(getDebtorAgent(firstPmt))
                .build();
    }

    /**
     * Gets debtor agent from first transaction.
     */
    private com.fednow.iso20022.domain.common.AgentIdentification getDebtorAgent(
            Pain008.PaymentInformation pmtInf) {
        if (pmtInf.getDirectDebitTransactionInformation() != null &&
            !pmtInf.getDirectDebitTransactionInformation().isEmpty()) {
            return pmtInf.getDirectDebitTransactionInformation().get(0).getDebtorAgent();
        }
        return null;
    }

    /**
     * Builds direct debit transactions.
     */
    private List<Pacs003.DirectDebitTransactionInformation> buildDirectDebitTransactions(
            Pain008 source, ConverterContext context) {

        List<Pacs003.DirectDebitTransactionInformation> transactions = new ArrayList<>();

        for (Pain008.PaymentInformation pmtInf : source.getPaymentInformation()) {
            for (Pain008.DirectDebitTransactionInformation sourceTxn :
                    pmtInf.getDirectDebitTransactionInformation()) {

                Pacs003.DirectDebitTransactionInformation pacs003Txn =
                        buildDirectDebitTransaction(sourceTxn, pmtInf, context);
                transactions.add(pacs003Txn);
            }
        }

        return transactions;
    }

    /**
     * Builds a single direct debit transaction.
     */
    private Pacs003.DirectDebitTransactionInformation buildDirectDebitTransaction(
            Pain008.DirectDebitTransactionInformation sourceTxn,
            Pain008.PaymentInformation pmtInf,
            ConverterContext context) {

        return Pacs003.DirectDebitTransactionInformation.builder()
                // Payment identification
                .paymentIdentification(com.fednow.iso20022.domain.common.PaymentIdentification.builder()
                        .instructionId(idGenerator.generateInstructionId())
                        .endToEndId(sourceTxn.getPaymentIdentification().getEndToEndId())
                        .transactionId(idGenerator.generateTransactionId())
                        .uetr(UUID.randomUUID().toString())
                        .build())
                // Amounts
                .interbankSettlementAmount(sourceTxn.getInstructedAmount())
                .interbankSettlementDate(context.getCurrentDate())
                .instructedAmount(sourceTxn.getInstructedAmount())
                // Charge bearer
                .chargeBearerCode("SLEV") // Service level for direct debit
                // Direct debit transaction (mandate info)
                .directDebitTransaction(buildDirectDebitTransaction(sourceTxn))
                // Creditor (collector) - from payment info
                .creditor(pmtInf.getCreditor())
                .creditorAccount(pmtInf.getCreditorAccount())
                .creditorAgent(pmtInf.getCreditorAgent())
                .ultimateCreditor(sourceTxn.getUltimateCreditor())
                // Debtor (being debited) - from transaction info
                .debtor(sourceTxn.getDebtor())
                .debtorAccount(sourceTxn.getDebtorAccount())
                .debtorAgent(sourceTxn.getDebtorAgent())
                .ultimateDebtor(sourceTxn.getUltimateDebtor())
                // Agents
                .instructingAgent(pmtInf.getCreditorAgent())
                .instructedAgent(sourceTxn.getDebtorAgent())
                // Purpose and remittance
                .purpose(convertPurpose(sourceTxn.getPurpose()))
                .remittanceInformation(sourceTxn.getRemittanceInformation())
                // Settlement info
                .settlementInformation(buildSettlementInformation())
                // Payment type info
                .paymentTypeInformation(buildPaymentTypeInformation(pmtInf))
                .build();
    }

    /**
     * Builds direct debit transaction with mandate info.
     */
    private Pacs003.DirectDebitTransaction buildDirectDebitTransaction(
            Pain008.DirectDebitTransactionInformation sourceTxn) {

        Pain008.DirectDebitTransaction sourceDD = sourceTxn.getDirectDebitTransaction();

        return Pacs003.DirectDebitTransaction.builder()
                .mandateRelatedInformation(buildMandateInfo(
                        sourceDD.getMandateRelatedInformation()))
                .creditorSchemeIdentification(sourceDD.getCreditorSchemeIdentification())
                .build();
    }

    /**
     * Builds mandate related information.
     */
    private Pacs003.MandateRelatedInformation buildMandateInfo(
            Pain008.MandateRelatedInformation sourceMandate) {

        return Pacs003.MandateRelatedInformation.builder()
                .mandateId(sourceMandate.getMandateId())
                .dateOfSignature(sourceMandate.getDateOfSignature())
                .amendmentIndicator(sourceMandate.getAmendmentIndicator())
                .originalMandateId(sourceMandate.getOriginalMandateId())
                .originalCreditorSchemeId(sourceMandate.getOriginalCreditorSchemeId())
                .build();
    }

    /**
     * Builds settlement information for FedNow.
     */
    private SettlementInformation buildSettlementInformation() {
        return SettlementInformation.builder()
                .settlementMethod("INDA") // Instructed Agent
                .clearingSystem("FDW")    // FedNow
                .build();
    }

    /**
     * Builds payment type information.
     */
    private Pacs003.PaymentTypeInformation buildPaymentTypeInformation(
            Pain008.PaymentInformation pmtInf) {

        if (pmtInf.getPaymentTypeInformation() == null) {
            return null;
        }

        Pain008.PaymentTypeInformation sourcePti = pmtInf.getPaymentTypeInformation();

        return Pacs003.PaymentTypeInformation.builder()
                .instructionPriority(sourcePti.getInstructionPriority())
                .serviceLevel(sourcePti.getServiceLevel())
                .localInstrument(sourcePti.getLocalInstrument())
                .sequenceType(sourcePti.getSequenceType())
                .categoryPurpose(sourcePti.getCategoryPurpose())
                .clearingChannel("RTGS") // FedNow uses RTGS
                .build();
    }

    /**
     * Converts purpose from pain.008 to pacs.003 format.
     */
    private Pacs003.Purpose convertPurpose(Pain008.Purpose sourcePurpose) {
        if (sourcePurpose == null) {
            return null;
        }

        return Pacs003.Purpose.builder()
                .code(sourcePurpose.getCode())
                .proprietary(sourcePurpose.getProprietary())
                .build();
    }
}
