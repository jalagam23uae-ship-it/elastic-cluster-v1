package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.camt.Camt054;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs008ToCamt054Converter - Account Notification Converter
 *
 * Converts pacs.008 (FI to FI Customer Credit Transfer) to camt.054 (Debit Credit Notification).
 *
 * This converter generates real-time account notifications for customers:
 * - Credit notifications (CRDT) for received payments
 * - Debit notifications (DBIT) for sent payments
 * - Booked (BOOK) status for settled transactions
 *
 * Message Flow:
 * Bank → Customer: pacs.008 → camt.054 (account notification)
 *
 * Notification Types:
 * - CRDT (Credit): Funds received - balance increases
 * - DBIT (Debit): Funds sent - balance decreases
 *
 * Bank Transaction Code:
 * - PMNT-RCDT: Received credit transfer (incoming payment)
 * - PMNT-ICDT: Issued credit transfer (outgoing payment)
 */
@Slf4j
@Component
public class Pacs008ToCamt054Converter extends AbstractMessageConverter<Pacs008, Camt054> {

    public Pacs008ToCamt054Converter() {
        super(Pacs008.class, Camt054.class, "Pacs008ToCamt054Converter");
    }

    @Override
    protected Mono<Camt054> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → camt.054 conversion");

        // Generate notification message ID
        enrichContextWithIds(context, "NTF", false, false);

        return Mono.fromCallable(() -> {
            // Determine if this is credit or debit notification based on context
            Boolean isCreditNotification = context.getAttribute("isCreditNotification",
                    Boolean.class);
            if (isCreditNotification == null) {
                isCreditNotification = true; // Default to credit (received payment)
            }

            // Build camt.054
            Camt054 camt054 = Camt054.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .notification(buildNotifications(source, context, isCreditNotification))
                    .build();

            logStep("Successfully created camt.054 account notification");
            return camt054;
        });
    }

    /**
     * Builds group header for camt.054.
     *
     * @param source pacs.008
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs008 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds account notifications.
     *
     * @param source pacs.008
     * @param context converter context
     * @param isCreditNotification whether this is a credit or debit notification
     * @return list of account notifications
     */
    private List<Camt054.AccountNotification> buildNotifications(
            Pacs008 source, ConverterContext context, boolean isCreditNotification) {

        List<Camt054.AccountNotification> notifications = new ArrayList<>();

        // Group transactions by account (for simplicity, one notification)
        Camt054.AccountNotification notification = Camt054.AccountNotification.builder()
                .identification(idGenerator.generateNotificationMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .account(buildAccount(source, isCreditNotification))
                .entry(buildEntries(source, context, isCreditNotification))
                .build();

        notifications.add(notification);

        return notifications;
    }

    /**
     * Builds account information.
     *
     * @param source pacs.008
     * @param isCreditNotification whether credit or debit
     * @return cash account
     */
    private Camt054.CashAccount buildAccount(Pacs008 source, boolean isCreditNotification) {
        // Get first transaction for account info
        Pacs008.CreditTransferTransactionInformation firstTxn =
                source.getCreditTransferTransactionInformation() != null &&
                        !source.getCreditTransferTransactionInformation().isEmpty()
                        ? source.getCreditTransferTransactionInformation().get(0)
                        : null;

        if (firstTxn == null) {
            return null;
        }

        // For credit notification, use creditor account
        // For debit notification, use debtor account
        return Camt054.CashAccount.builder()
                .identification(isCreditNotification
                        ? firstTxn.getCreditorAccount()
                        : firstTxn.getDebtorAccount())
                .type("CACC") // Current account
                .currency("USD")
                .owner(isCreditNotification
                        ? firstTxn.getCreditor()
                        : firstTxn.getDebtor())
                .servicer(context.getBankContext().getBankIdentification())
                .build();
    }

    /**
     * Builds report entries.
     *
     * @param source pacs.008
     * @param context converter context
     * @param isCreditNotification whether credit or debit
     * @return list of report entries
     */
    private List<Camt054.ReportEntry> buildEntries(
            Pacs008 source, ConverterContext context, boolean isCreditNotification) {

        List<Camt054.ReportEntry> entries = new ArrayList<>();

        for (Pacs008.CreditTransferTransactionInformation txn :
                source.getCreditTransferTransactionInformation()) {

            Camt054.ReportEntry entry = buildEntry(txn, context, isCreditNotification);
            entries.add(entry);
        }

        return entries;
    }

    /**
     * Builds a single report entry.
     *
     * @param txn transaction from pacs.008
     * @param context converter context
     * @param isCreditNotification whether credit or debit
     * @return report entry
     */
    private Camt054.ReportEntry buildEntry(
            Pacs008.CreditTransferTransactionInformation txn,
            ConverterContext context,
            boolean isCreditNotification) {

        return Camt054.ReportEntry.builder()
                .entryReference(idGenerator.generateEntryReference())
                .amount(txn.getInterbankSettlementAmount())
                // Credit or Debit indicator
                .creditDebitIndicator(isCreditNotification ? "CRDT" : "DBIT")
                // Entry is booked (final)
                .status("BOOK")
                // Booking date
                .bookingDate(Camt054.DateAndDateTime.builder()
                        .date(context.getCurrentDate())
                        .build())
                // Value date (when funds are available)
                .valueDate(Camt054.DateAndDateTime.builder()
                        .date(context.getCurrentDate())
                        .build())
                // Account servicer reference
                .accountServicerReference(idGenerator.generateAccountServicerReference())
                // Bank transaction code
                .bankTransactionCode(isCreditNotification
                        ? Camt054.BankTransactionCode.FedNowCodes.receivedCreditTransfer()
                        : Camt054.BankTransactionCode.FedNowCodes.issuedCreditTransfer())
                // Entry details
                .entryDetails(buildEntryDetails(txn))
                .build();
    }

    /**
     * Builds entry details.
     *
     * @param txn transaction from pacs.008
     * @return list of entry details
     */
    private List<Camt054.EntryDetails> buildEntryDetails(
            Pacs008.CreditTransferTransactionInformation txn) {

        List<Camt054.EntryDetails> detailsList = new ArrayList<>();

        Camt054.EntryDetails details = Camt054.EntryDetails.builder()
                .transactionDetails(buildTransactionDetails(txn))
                .build();

        detailsList.add(details);

        return detailsList;
    }

    /**
     * Builds transaction details.
     *
     * @param txn transaction from pacs.008
     * @return list of transaction details
     */
    private List<Camt054.TransactionDetails> buildTransactionDetails(
            Pacs008.CreditTransferTransactionInformation txn) {

        List<Camt054.TransactionDetails> txnDetailsList = new ArrayList<>();

        Camt054.TransactionDetails txnDetails = Camt054.TransactionDetails.builder()
                // References
                .references(Camt054.TransactionReferences.builder()
                        .messageIdentification(txn.getPaymentIdentification().getInstructionId())
                        .instructionIdentification(txn.getPaymentIdentification().getInstructionId())
                        .endToEndIdentification(txn.getPaymentIdentification().getEndToEndId())
                        .transactionIdentification(txn.getPaymentIdentification().getTransactionId())
                        .uetr(txn.getPaymentIdentification().getUetr())
                        .build())
                // Amount details
                .amountDetails(Camt054.AmountDetails.builder()
                        .instructedAmount(txn.getInstructedAmount())
                        .transactionAmount(txn.getInterbankSettlementAmount())
                        .interbankSettlementAmount(txn.getInterbankSettlementAmount())
                        .build())
                // Related parties
                .relatedParties(Camt054.RelatedParties.builder()
                        .debtor(txn.getDebtor())
                        .debtorAccount(txn.getDebtorAccount())
                        .ultimateDebtor(txn.getUltimateDebtor())
                        .creditor(txn.getCreditor())
                        .creditorAccount(txn.getCreditorAccount())
                        .ultimateCreditor(txn.getUltimateCreditor())
                        .build())
                // Related agents
                .relatedAgents(Camt054.RelatedAgents.builder()
                        .debtorAgent(txn.getDebtorAgent())
                        .creditorAgent(txn.getCreditorAgent())
                        .build())
                // Purpose
                .purpose(txn.getPurpose() != null
                        ? Camt054.Purpose.builder()
                        .code(txn.getPurpose().getCode())
                        .proprietary(txn.getPurpose().getProprietary())
                        .build()
                        : null)
                // Remittance information
                .remittanceInformation(txn.getRemittanceInformation())
                .build();

        txnDetailsList.add(txnDetails);

        return txnDetailsList;
    }

    /**
     * Helper method to create camt.054 for credit notification (received payment).
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Camt054>
     */
    public Mono<Camt054> createCreditNotification(Pacs008 source, ConverterContext context) {
        context.setAttribute("isCreditNotification", true);
        return convert(source, context);
    }

    /**
     * Helper method to create camt.054 for debit notification (sent payment).
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Camt054>
     */
    public Mono<Camt054> createDebitNotification(Pacs008 source, ConverterContext context) {
        context.setAttribute("isCreditNotification", false);
        return convert(source, context);
    }
}
