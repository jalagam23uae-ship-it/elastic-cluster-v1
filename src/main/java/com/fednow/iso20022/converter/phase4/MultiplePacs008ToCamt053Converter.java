package com.fednow.iso20022.converter.phase4;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.camt.Camt053;
import com.fednow.iso20022.domain.common.Amount;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MultiplePacs008ToCamt053Converter - Multiple Payments to Official Account Statement
 *
 * Converts multiple pacs.008 messages into a single camt.053 (Official Account Statement).
 *
 * This converter aggregates multiple payment transactions into an official, legally binding
 * account statement, typically used for:
 *
 * **Official Statement Generation:**
 * - Daily account statements (end-of-day)
 * - Monthly account statements (end-of-month)
 * - Quarterly statements (regulatory reporting)
 * - Annual statements (tax reporting)
 *
 * **Legal and Compliance:**
 * - Auditable transaction records
 * - Regulatory compliance (SOX, FCPA, etc.)
 * - Tax documentation
 * - Legal proceedings evidence
 * - Dispute resolution
 *
 * **Use Cases:**
 * 1. **Daily Statements**: All payments received/sent during business day
 * 2. **Month-End Close**: Official monthly statement for reconciliation
 * 3. **Audit Trail**: Complete transaction history with balances
 * 4. **Customer Portal**: Official statement downloads
 * 5. **Archive/Records**: Legal retention and compliance
 *
 * **Statement Features:**
 * - Sequential statement numbering
 * - Opening and closing balances (official)
 * - Transaction summary (count, total credits, total debits)
 * - Complete transaction details
 * - Chronological ordering
 * - Legal/audit markers
 *
 * **Difference from camt.052:**
 * - camt.053: Official STATEMENT (legal record, auditable, periodic)
 * - camt.052: Account REPORT (informational, can be intraday)
 *
 * Message Flow:
 * Multiple pacs.008 → End-of-period aggregation → camt.053 (official statement) → Customer
 */
@Slf4j
@Component
public class MultiplePacs008ToCamt053Converter extends AbstractMessageConverter<List<Pacs008>, Camt053> {

    public MultiplePacs008ToCamt053Converter() {
        super(List.class, Camt053.class, "MultiplePacs008ToCamt053Converter");
    }

    @Override
    protected Mono<Camt053> doConvert(List<Pacs008> sources, ConverterContext context) {
        logStep("Starting multiple pacs.008 → camt.053 conversion (payments → account statement)");

        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one pacs.008 message is required");
        }

        // Generate message ID for statement
        enrichContextWithIds(context, "STMT", false, false);

        return Mono.fromCallable(() -> {
            // Get statement parameters from context
            String accountNumber = context.getAttribute("accountNumber", String.class);
            if (accountNumber == null) {
                throw new IllegalStateException("Account number must be provided");
            }

            String statementNumber = context.getAttribute("statementNumber", String.class);
            if (statementNumber == null) {
                statementNumber = generateStatementNumber();
            }

            String frequency = context.getAttribute("frequency", String.class);
            if (frequency == null) {
                frequency = "DAIL"; // Daily by default
            }

            // Determine statement period
            ZonedDateTime fromTime = context.getAttribute("statementFromTime", ZonedDateTime.class);
            ZonedDateTime toTime = context.getAttribute("statementToTime", ZonedDateTime.class);

            if (fromTime == null) {
                fromTime = context.getCurrentTimestamp().minusDays(1);
            }
            if (toTime == null) {
                toTime = context.getCurrentTimestamp();
            }

            // Build camt.053
            Camt053 camt053 = Camt053.builder()
                    .groupHeader(buildGroupHeader(context))
                    .statement(buildOfficialStatements(sources, context, accountNumber,
                            statementNumber, frequency, fromTime, toTime))
                    .build();

            logStep(String.format("Successfully created official camt.053 statement #%s with %d payments",
                    statementNumber, countTotalTransactions(sources)));
            return camt053;
        });
    }

    /**
     * Counts total transactions.
     */
    private int countTotalTransactions(List<Pacs008> sources) {
        return sources.stream()
                .mapToInt(p -> p.getCreditTransferTransactionInformation().size())
                .sum();
    }

    /**
     * Generates statement number.
     */
    private String generateStatementNumber() {
        return idGenerator.generateTransactionId();
    }

    /**
     * Builds group header.
     */
    private GroupHeader buildGroupHeader(ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds official account statements.
     */
    private List<Camt053.AccountStatement> buildOfficialStatements(
            List<Pacs008> sources,
            ConverterContext context,
            String accountNumber,
            String statementNumber,
            String frequency,
            ZonedDateTime fromTime,
            ZonedDateTime toTime) {

        List<Camt053.AccountStatement> statements = new ArrayList<>();

        Camt053.AccountStatement statement = Camt053.AccountStatement.builder()
                .statementId(generateStatementId())
                .statementNumber(statementNumber)
                .creationDateTime(context.getCurrentTimestamp())
                .fromDateTime(fromTime)
                .toDateTime(toTime)
                .frequency(frequency)
                .account(buildAccountIdentification(accountNumber))
                .accountOwner(getAccountOwner(sources.get(0), context))
                .accountServicer(context.getBankContext().getAgentIdentification())
                .balance(buildOfficialBalances(sources, context, fromTime, toTime))
                .transactionsSummary(buildTransactionsSummary(sources))
                .entry(buildStatementEntries(sources, context, accountNumber))
                .build();

        statements.add(statement);

        return statements;
    }

    /**
     * Generates statement ID.
     */
    private String generateStatementId() {
        return idGenerator.generateMessageId("STMT");
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
     * Gets account owner.
     */
    private com.fednow.iso20022.domain.common.PartyIdentification getAccountOwner(
            Pacs008 firstPacs008,
            ConverterContext context) {

        Pacs008.CreditTransferTransactionInformation txn =
                firstPacs008.getCreditTransferTransactionInformation().get(0);

        return txn.getCreditor();
    }

    /**
     * Builds official balances.
     */
    private List<Camt053.Balance> buildOfficialBalances(
            List<Pacs008> sources,
            ConverterContext context,
            ZonedDateTime fromTime,
            ZonedDateTime toTime) {

        List<Camt053.Balance> balances = new ArrayList<>();

        // Opening booked balance
        Amount openingBalance = context.getAttribute("openingBalance", Amount.class);
        if (openingBalance == null) {
            openingBalance = Amount.builder()
                    .value(BigDecimal.ZERO)
                    .currency("USD")
                    .build();
        }

        balances.add(Camt053.Balance.builder()
                .type("OPBD")
                .amount(openingBalance)
                .creditDebitIndicator(determineCreditDebit(openingBalance))
                .dateTime(fromTime)
                .build();
        }

        // Calculate closing booked balance
        BigDecimal totalCredits = calculateTotalAmount(sources);
        BigDecimal closingBalanceValue = openingBalance.getValue().add(totalCredits);

        Amount closingBalance = Amount.builder()
                .value(closingBalanceValue)
                .currency("USD")
                .build();

        balances.add(Camt053.Balance.builder()
                .type("CLBD")
                .amount(closingBalance)
                .creditDebitIndicator(determineCreditDebit(closingBalance))
                .dateTime(toTime)
                .build());

        // Opening available balance (if provided)
        Amount openingAvailBalance = context.getAttribute("openingAvailableBalance", Amount.class);
        if (openingAvailBalance != null) {
            balances.add(Camt053.Balance.builder()
                    .type("OPAV")
                    .amount(openingAvailBalance)
                    .creditDebitIndicator(determineCreditDebit(openingAvailBalance))
                    .dateTime(fromTime)
                    .build());
        }

        // Closing available balance (if provided)
        Amount closingAvailBalance = context.getAttribute("closingAvailableBalance", Amount.class);
        if (closingAvailBalance != null) {
            balances.add(Camt053.Balance.builder()
                    .type("CLAV")
                    .amount(closingAvailBalance)
                    .creditDebitIndicator(determineCreditDebit(closingAvailBalance))
                    .dateTime(toTime)
                    .build());
        }

        return balances;
    }

    /**
     * Calculates total amount.
     */
    private BigDecimal calculateTotalAmount(List<Pacs008> sources) {
        BigDecimal total = BigDecimal.ZERO;

        for (Pacs008 pacs008 : sources) {
            for (Pacs008.CreditTransferTransactionInformation txn :
                    pacs008.getCreditTransferTransactionInformation()) {
                total = total.add(txn.getInterbankSettlementAmount().getValue());
            }
        }

        return total;
    }

    /**
     * Determines credit/debit indicator.
     */
    private String determineCreditDebit(Amount amount) {
        return amount.getValue().signum() >= 0 ? "CRDT" : "DBIT";
    }

    /**
     * Builds transactions summary.
     */
    private Camt053.TransactionsSummary buildTransactionsSummary(List<Pacs008> sources) {
        int totalEntries = countTotalTransactions(sources);
        BigDecimal totalCreditAmount = calculateTotalAmount(sources);

        return Camt053.TransactionsSummary.builder()
                .totalEntries(totalEntries)
                .totalCreditEntries(totalEntries) // All are credits in this case
                .totalCreditAmount(Amount.builder()
                        .value(totalCreditAmount)
                        .currency("USD")
                        .build())
                .totalDebitEntries(0)
                .totalDebitAmount(Amount.builder()
                        .value(BigDecimal.ZERO)
                        .currency("USD")
                        .build())
                .build();
    }

    /**
     * Builds statement entries.
     */
    private List<Camt053.TransactionEntry> buildStatementEntries(
            List<Pacs008> sources,
            ConverterContext context,
            String accountNumber) {

        List<Camt053.TransactionEntry> entries = new ArrayList<>();

        for (Pacs008 pacs008 : sources) {
            for (Pacs008.CreditTransferTransactionInformation txn :
                    pacs008.getCreditTransferTransactionInformation()) {

                Camt053.TransactionEntry entry = buildEntry(txn, context, accountNumber);
                entries.add(entry);
            }
        }

        return entries;
    }

    /**
     * Builds a single transaction entry.
     */
    private Camt053.TransactionEntry buildEntry(
            Pacs008.CreditTransferTransactionInformation txn,
            ConverterContext context,
            String accountNumber) {

        return Camt053.TransactionEntry.builder()
                .entryReference(generateEntryReference())
                .amount(txn.getInterbankSettlementAmount())
                .creditDebitIndicator("CRDT")
                .reversalIndicator(false)
                .status("BOOK")
                .bookingDate(context.getCurrentDate())
                .valueDate(context.getCurrentDate())
                .accountServicerReference(generateAccountServicerReference())
                .bankTransactionCode(buildBankTransactionCode())
                .entryDetails(buildEntryDetails(txn))
                .additionalEntryInformation("FedNow instant payment received")
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
    private Camt053.BankTransactionCode buildBankTransactionCode() {
        return Camt053.BankTransactionCode.builder()
                .domainCode("PMNT")
                .familyCode("RCDT")
                .subFamilyCode("ESCT")
                .build();
    }

    /**
     * Builds entry details.
     */
    private List<Camt053.TransactionDetails> buildEntryDetails(
            Pacs008.CreditTransferTransactionInformation txn) {

        List<Camt053.TransactionDetails> details = new ArrayList<>();

        Camt053.TransactionDetails txnDetails = Camt053.TransactionDetails.builder()
                .references(buildTransactionReferences(txn))
                .amount(txn.getInterbankSettlementAmount())
                .creditDebitIndicator("CRDT")
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
    private Camt053.TransactionReferences buildTransactionReferences(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt053.TransactionReferences.builder()
                .instructionId(txn.getPaymentIdentification().getInstructionId())
                .endToEndId(txn.getPaymentIdentification().getEndToEndId())
                .transactionId(txn.getPaymentIdentification().getTransactionId())
                .uetr(txn.getPaymentIdentification().getUetr())
                .build();
    }

    /**
     * Builds related parties.
     */
    private Camt053.RelatedParties buildRelatedParties(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt053.RelatedParties.builder()
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
    private Camt053.RelatedAgents buildRelatedAgents(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Camt053.RelatedAgents.builder()
                .debtorAgent(txn.getDebtorAgent())
                .creditorAgent(txn.getCreditorAgent())
                .instructingAgent(txn.getInstructingAgent())
                .instructedAgent(txn.getInstructedAgent())
                .build();
    }
}
