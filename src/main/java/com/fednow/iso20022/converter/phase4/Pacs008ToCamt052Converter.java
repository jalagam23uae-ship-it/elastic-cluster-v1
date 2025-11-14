package com.fednow.iso20022.converter.phase4;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.camt.Camt052;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pacs008ToCamt052Converter - Payment to Account Report
 *
 * Converts pacs.008 (FI to FI Customer Credit Transfer) to camt.052 (Bank to Customer Account Report).
 *
 * This converter transforms a payment transaction into an account report entry,
 * typically used for real-time account activity reporting to customers.
 *
 * Purpose:
 * - Provide customers with real-time payment visibility
 * - Update account activity reports immediately after payment processing
 * - Support intraday cash management and treasury operations
 * - Enable instant payment reconciliation
 *
 * Report Context:
 * - **Intraday Reports**: Generated throughout business day as payments arrive
 * - **Transaction Notifications**: Real-time updates to corporate banking portals
 * - **Cash Position Updates**: Immediate balance and activity visibility
 * - **Liquidity Management**: Real-time cash flow tracking
 *
 * Generated Entry Information:
 * - Credit/debit indicator based on account perspective
 * - Booking date and value date
 * - Complete transaction details (parties, agents, remittance)
 * - Bank transaction codes for categorization
 * - Current account balance (if available)
 *
 * Message Flow:
 * FedNow → pacs.008 (payment received) → Bank processes → camt.052 (account report) → Customer
 */
@Slf4j
@Component
public class Pacs008ToCamt052Converter extends AbstractMessageConverter<Pacs008, Camt052> {

    public Pacs008ToCamt052Converter() {
        super(Pacs008.class, Camt052.class, "Pacs008ToCamt052Converter");
    }

    @Override
    protected Mono<Camt052> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → camt.052 conversion (payment → account report)");

        // Generate message ID for report
        enrichContextWithIds(context, "RPT", false, false);

        return Mono.fromCallable(() -> {
            // Determine account perspective (creditor receiving funds)
            String accountNumber = context.getAttribute("accountNumber", String.class);
            if (accountNumber == null) {
                throw new IllegalStateException(
                        "Account number must be provided for account report generation");
            }

            // Build camt.052
            Camt052 camt052 = Camt052.builder()
                    .groupHeader(buildGroupHeader(context))
                    .report(buildAccountReports(source, context, accountNumber))
                    .build();

            logStep("Successfully created camt.052 account report");
            return camt052;
        });
    }

    /**
     * Builds group header for camt.052.
     */
    private GroupHeader buildGroupHeader(ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds account reports.
     */
    private List<Camt052.AccountReport> buildAccountReports(
            Pacs008 source,
            ConverterContext context,
            String accountNumber) {

        List<Camt052.AccountReport> reports = new ArrayList<>();

        Camt052.AccountReport report = Camt052.AccountReport.builder()
                .reportId(generateReportId())
                .creationDateTime(context.getCurrentTimestamp())
                .fromDateTime(context.getCurrentTimestamp().minusMinutes(1))
                .toDateTime(context.getCurrentTimestamp())
                .account(buildAccountIdentification(accountNumber))
                .accountOwner(getAccountOwner(source, context))
                .accountServicer(context.getBankContext().getAgentIdentification())
                .balance(buildBalances(context))
                .entry(buildEntries(source, context, accountNumber))
                .build();

        reports.add(report);

        return reports;
    }

    /**
     * Generates report ID.
     */
    private String generateReportId() {
        return idGenerator.generateMessageId("RPT");
    }

    /**
     * Builds account identification.
     */
    private com.fednow.iso20022.domain.common.AccountIdentification buildAccountIdentification(
            String accountNumber) {
        return com.fednow.iso20022.domain.common.AccountIdentification.builder()
                .identification(accountNumber)
                .build();
    }

    /**
     * Gets account owner from payment.
     */
    private com.fednow.iso20022.domain.common.PartyIdentification getAccountOwner(
            Pacs008 source,
            ConverterContext context) {

        // For credit entry, account owner is creditor
        Pacs008.CreditTransferTransactionInformation txn =
                source.getCreditTransferTransactionInformation().get(0);

        return txn.getCreditor();
    }

    /**
     * Builds balances (if available in context).
     */
    private List<Camt052.Balance> buildBalances(ConverterContext context) {
        List<Camt052.Balance> balances = new ArrayList<>();

        // Opening booked balance (if available)
        com.fednow.iso20022.domain.common.Amount openingBalance =
                context.getAttribute("openingBalance", com.fednow.iso20022.domain.common.Amount.class);
        if (openingBalance != null) {
            balances.add(Camt052.Balance.builder()
                    .type("OPBD")
                    .amount(openingBalance)
                    .creditDebitIndicator(determineCreditDebit(openingBalance))
                    .dateTime(context.getCurrentTimestamp().minusHours(1))
                    .build());
        }

        // Interim booked balance (if available)
        com.fednow.iso20022.domain.common.Amount currentBalance =
                context.getAttribute("currentBalance", com.fednow.iso20022.domain.common.Amount.class);
        if (currentBalance != null) {
            balances.add(Camt052.Balance.builder()
                    .type("ITBD")
                    .amount(currentBalance)
                    .creditDebitIndicator(determineCreditDebit(currentBalance))
                    .dateTime(context.getCurrentTimestamp())
                    .build());
        }

        return balances;
    }

    /**
     * Determines credit/debit indicator from amount.
     */
    private String determineCreditDebit(com.fednow.iso20022.domain.common.Amount amount) {
        return amount.getValue().signum() >= 0 ? "CRDT" : "DBIT";
    }

    /**
     * Builds transaction entries.
     */
    private List<Camt052.TransactionEntry> buildEntries(
            Pacs008 source,
            ConverterContext context,
            String accountNumber) {

        List<Camt052.TransactionEntry> entries = new ArrayList<>();

        for (Pacs008.CreditTransferTransactionInformation txn :
                source.getCreditTransferTransactionInformation()) {

            Camt052.TransactionEntry entry = buildEntry(txn, context, accountNumber);
            entries.add(entry);
        }

        return entries;
    }

    /**
     * Builds a single transaction entry.
     */
    private Camt052.TransactionEntry buildEntry(
            Pacs008.CreditTransferTransactionInformation txn,
            ConverterContext context,
            String accountNumber) {

        return Camt052.TransactionEntry.builder()
                .entryReference(generateEntryReference())
                .amount(txn.getInterbankSettlementAmount())
                // Credit entry (receiving funds)
                .creditDebitIndicator("CRDT")
                .reversalIndicator(false)
                .status("BOOK") // Booked
                .bookingDate(context.getCurrentDate())
                .valueDate(context.getCurrentDate())
                .accountServicerReference(generateAccountServicerReference())
                .bankTransactionCode(buildBankTransactionCode())
                .entryDetails(buildEntryDetails(txn))
                .build();
    }

    /**
     * Generates entry reference.
     */
    private String generateEntryReference() {
        return idGenerator.generateTransactionId();
    }

    /**
     * Generates account servicer reference.
     */
    private String generateAccountServicerReference() {
        return String.format("ASR-%s", idGenerator.generateTransactionId());
    }

    /**
     * Builds bank transaction code.
     */
    private Camt052.BankTransactionCode buildBankTransactionCode() {
        return Camt052.BankTransactionCode.builder()
                .domainCode("PMNT")    // Payment
                .familyCode("RCDT")    // Received credit transfer
                .subFamilyCode("ESCT") // SEPA/domestic credit transfer
                .build();
    }

    /**
     * Builds entry details.
     */
    private List<Camt052.TransactionDetails> buildEntryDetails(
            Pacs008.CreditTransferTransactionInformation txn) {

        List<Camt052.TransactionDetails> details = new ArrayList<>();

        Camt052.TransactionDetails txnDetails = Camt052.TransactionDetails.builder()
                .references(buildTransactionReferences(txn))
                .amount(txn.getInterbankSettlementAmount())
                .relatedParties(buildRelatedParties(txn))
                .relatedAgents(buildRelatedAgents(txn))
                .remittanceInformation(txn.getRemittanceInformation())
                .build();

        details.add(txnDetails);

        return details;
    }

    /**
     * Builds transaction references.
     */
    private Camt052.TransactionReferences buildTransactionReferences(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt052.TransactionReferences.builder()
                .instructionId(txn.getPaymentIdentification().getInstructionId())
                .endToEndId(txn.getPaymentIdentification().getEndToEndId())
                .transactionId(txn.getPaymentIdentification().getTransactionId())
                .uetr(txn.getPaymentIdentification().getUetr())
                .build();
    }

    /**
     * Builds related parties.
     */
    private Camt052.RelatedParties buildRelatedParties(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt052.RelatedParties.builder()
                .debtor(txn.getDebtor())
                .debtorAccount(txn.getDebtorAccount())
                .ultimateDebtor(txn.getUltimateDebtor())
                .creditor(txn.getCreditor())
                .creditorAccount(txn.getCreditorAccount())
                .ultimateCreditor(txn.getUltimateCreditor())
                .build();
    }

    /**
     * Builds related agents.
     */
    private Camt052.RelatedAgents buildRelatedAgents(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt052.RelatedAgents.builder()
                .debtorAgent(txn.getDebtorAgent())
                .creditorAgent(txn.getCreditorAgent())
                .instructingAgent(txn.getInstructingAgent())
                .instructedAgent(txn.getInstructedAgent())
                .build();
    }
}
