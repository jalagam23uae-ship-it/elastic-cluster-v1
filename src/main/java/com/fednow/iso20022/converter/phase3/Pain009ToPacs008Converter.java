package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.*;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain009;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pain009ToPacs008Converter - Mandate Setup to Initial Payment
 *
 * Converts pain.009 (Mandate Initiation Request) to pacs.008 (FI to FI Customer Credit Transfer).
 *
 * This converter handles the scenario where establishing a new mandate triggers an
 * immediate initial payment. This is common in:
 *
 * 1. **Subscription Services with Initial Payment**
 *    - Customer signs up for service
 *    - Mandate established for recurring payments
 *    - First payment collected immediately
 *
 * 2. **Membership Fees**
 *    - Customer joins organization
 *    - Mandate for annual dues
 *    - Initial membership fee paid immediately
 *
 * 3. **Insurance Policies**
 *    - Policy activation
 *    - Mandate for premium collections
 *    - First premium paid on signup
 *
 * 4. **Loan Setup Fees**
 *    - Loan agreement signed
 *    - Mandate for repayments
 *    - Processing fee collected immediately
 *
 * Business Logic:
 * - Mandate approval triggers immediate payment collection
 * - Payment amount taken from context (setup fee, initial payment, etc.)
 * - Mandate details embedded in payment for tracking
 * - Payment uses creditor details from mandate
 *
 * Note: This is NOT a standard direct debit collection (that would be pacs.003).
 * This is a credit transfer payment that happens as part of mandate setup.
 */
@Slf4j
@Component
public class Pain009ToPacs008Converter extends AbstractMessageConverter<Pain009, Pacs008> {

    public Pain009ToPacs008Converter() {
        super(Pain009.class, Pacs008.class, "Pain009ToPacs008Converter");
    }

    @Override
    protected Mono<Pacs008> doConvert(Pain009 source, ConverterContext context) {
        logStep("Starting pain.009 → pacs.008 conversion (mandate setup → initial payment)");

        // Generate message ID and UETR
        enrichContextWithIds(context, "MNDT", true, false);

        return Mono.fromCallable(() -> {
            // Validate pain.009
            validatePain009(source);

            // Get initial payment amount from context
            BigDecimal initialPaymentAmount = context.getAttribute(
                    "initialPaymentAmount", BigDecimal.class);
            if (initialPaymentAmount == null) {
                throw new IllegalStateException(
                        "Initial payment amount must be provided in context");
            }

            String paymentPurpose = context.getAttribute("paymentPurpose", String.class);
            if (paymentPurpose == null) {
                paymentPurpose = "Initial payment for mandate setup";
            }

            // Build pacs.008
            Pacs008 pacs008 = Pacs008.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .creditTransferTransactionInformation(
                            buildCreditTransferTransactions(source, context,
                                    initialPaymentAmount, paymentPurpose))
                    .build();

            logStep("Successfully created pacs.008 initial payment from mandate");
            return pacs008;
        });
    }

    /**
     * Validates pain.009 input.
     */
    private void validatePain009(Pain009 source) {
        if (source.getGroupHeader() == null) {
            throw new IllegalArgumentException("Group header is required");
        }

        if (source.getMandate() == null || source.getMandate().isEmpty()) {
            throw new IllegalArgumentException("Mandate information is required");
        }

        Pain009.MandateInformation mandate = source.getMandate().get(0);
        if (mandate.getMandateDetails() == null) {
            throw new IllegalArgumentException("Mandate details are required");
        }

        logStep("pain.009 validation passed");
    }

    /**
     * Builds group header for pacs.008.
     */
    private GroupHeader buildGroupHeader(Pain009 source, ConverterContext context) {
        Pain009.MandateInformation firstMandate = source.getMandate().get(0);
        Pain009.MandateDetails details = firstMandate.getMandateDetails();

        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Debtor agent (customer's bank) is instructing
                .instructingAgent(details.getDebtorAgent())
                // Creditor agent (service provider's bank) is instructed
                .instructedAgent(details.getCreditorAgent())
                .build();
    }

    /**
     * Builds credit transfer transactions.
     */
    private List<Pacs008.CreditTransferTransactionInformation> buildCreditTransferTransactions(
            Pain009 source,
            ConverterContext context,
            BigDecimal amount,
            String purpose) {

        List<Pacs008.CreditTransferTransactionInformation> transactions = new ArrayList<>();

        for (Pain009.MandateInformation mandate : source.getMandate()) {
            Pacs008.CreditTransferTransactionInformation txn =
                    buildCreditTransferTransaction(mandate, context, amount, purpose);
            transactions.add(txn);
        }

        return transactions;
    }

    /**
     * Builds a single credit transfer transaction.
     */
    private Pacs008.CreditTransferTransactionInformation buildCreditTransferTransaction(
            Pain009.MandateInformation mandate,
            ConverterContext context,
            BigDecimal amount,
            String purpose) {

        Pain009.MandateDetails details = mandate.getMandateDetails();

        return Pacs008.CreditTransferTransactionInformation.builder()
                // Payment identification
                .paymentIdentification(PaymentIdentification.builder()
                        .instructionId(idGenerator.generateInstructionId())
                        .endToEndId("MNDT-SETUP-" + mandate.getMandateId())
                        .transactionId(idGenerator.generateTransactionId())
                        .uetr(UUID.randomUUID().toString())
                        .build())
                // Amounts
                .interbankSettlementAmount(Amount.builder()
                        .value(amount)
                        .currency("USD")
                        .build())
                .interbankSettlementDate(context.getCurrentDate())
                .instructedAmount(Amount.builder()
                        .value(amount)
                        .currency("USD")
                        .build())
                // Settlement information
                .settlementInformation(SettlementInformation.builder()
                        .settlementMethod("INDA")
                        .clearingSystem("FDW")
                        .build())
                // Charge bearer
                .chargeBearerCode("SHAR")
                // Payment type
                .paymentTypeInformation(Pacs008.PaymentTypeInformation.builder()
                        .instructionPriority("NORM")
                        .clearingChannel("RTGS")
                        .serviceLevel("SDVA") // Same day value
                        .localInstrument("MNDT") // Mandate-related payment
                        .categoryPurpose("CASH")
                        .build())
                // Debtor (customer setting up mandate)
                .debtor(details.getDebtor())
                .debtorAccount(details.getDebtorAccount())
                .debtorAgent(details.getDebtorAgent())
                // Creditor (service provider)
                .creditor(details.getCreditor())
                .creditorAccount(details.getCreditorAccount())
                .creditorAgent(details.getCreditorAgent())
                // Agents
                .instructingAgent(details.getDebtorAgent())
                .instructedAgent(details.getCreditorAgent())
                // Remittance info
                .remittanceInformation(RemittanceInformation.builder()
                        .unstructured(List.of(
                                purpose,
                                "Mandate ID: " + mandate.getMandateId(),
                                "Mandate Type: " + details.getSequenceType()))
                        .build())
                .build();
    }

    /**
     * Helper method to create initial payment for subscription setup.
     */
    public Mono<Pacs008> createSubscriptionSetupPayment(
            Pain009 source,
            ConverterContext context,
            BigDecimal setupFee) {
        context.setAttribute("initialPaymentAmount", setupFee);
        context.setAttribute("paymentPurpose", "Subscription setup fee");
        return convert(source, context);
    }

    /**
     * Helper method to create initial payment for membership.
     */
    public Mono<Pacs008> createMembershipInitialPayment(
            Pain009 source,
            ConverterContext context,
            BigDecimal membershipFee) {
        context.setAttribute("initialPaymentAmount", membershipFee);
        context.setAttribute("paymentPurpose", "Initial membership fee");
        return convert(source, context);
    }

    /**
     * Helper method to create initial insurance premium payment.
     */
    public Mono<Pacs008> createInsuranceInitialPremium(
            Pain009 source,
            ConverterContext context,
            BigDecimal premiumAmount) {
        context.setAttribute("initialPaymentAmount", premiumAmount);
        context.setAttribute("paymentPurpose", "Initial insurance premium payment");
        return convert(source, context);
    }

    /**
     * Helper method to create loan setup fee payment.
     */
    public Mono<Pacs008> createLoanSetupFee(
            Pain009 source,
            ConverterContext context,
            BigDecimal setupFee) {
        context.setAttribute("initialPaymentAmount", setupFee);
        context.setAttribute("paymentPurpose", "Loan origination and setup fee");
        return convert(source, context);
    }
}
