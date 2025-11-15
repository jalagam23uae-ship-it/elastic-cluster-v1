package com.fednow.iso20022.service;

import com.fednow.iso20022.entity.enums.MessageStatus;
import com.fednow.iso20022.entity.enums.PaymentStatusCode;
import com.fednow.iso20022.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating analytics and reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final MessageRepository messageRepository;
    private final ConversionRepository conversionRepository;
    private final ValidationErrorRepository validationErrorRepository;
    private final PaymentStatusReportRepository paymentStatusReportRepository;
    private final SystemEventRepository systemEventRepository;
    private final ConverterMetricsRepository converterMetricsRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    /**
     * Generate system health report
     */
    public Mono<Map<String, Object>> generateSystemHealthReport() {
        log.debug("Generating system health report");
        
        return Mono.zip(
                messageRepository.countByStatus(MessageStatus.RECEIVED),
                messageRepository.countByStatus(MessageStatus.FAILED),
                messageRepository.countByStatus(MessageStatus.REJECTED),
                systemEventRepository.countBySeverity("FATAL"),
                systemEventRepository.countBySeverity("ERROR"),
                validationErrorRepository.countBySeverity("CRITICAL")
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            report.put("timestamp", Instant.now());
            report.put("messages_received", tuple.getT1());
            report.put("messages_failed", tuple.getT2());
            report.put("messages_rejected", tuple.getT3());
            report.put("fatal_events", tuple.getT4());
            report.put("error_events", tuple.getT5());
            report.put("critical_validation_errors", tuple.getT6());
            
            // Calculate health score (0-100)
            long totalIssues = tuple.getT2() + tuple.getT3() + tuple.getT4() + tuple.getT5() + tuple.getT6();
            int healthScore = totalIssues == 0 ? 100 : Math.max(0, 100 - (int) (totalIssues * 2));
            report.put("health_score", healthScore);
            report.put("status", healthScore >= 90 ? "HEALTHY" : healthScore >= 70 ? "WARNING" : "CRITICAL");
            
            log.info("System health report generated: healthScore={}, status={}", healthScore, report.get("status"));
            return report;
        });
    }

    /**
     * Generate converter performance report
     */
    public Mono<Map<String, Object>> generateConverterPerformanceReport(String converterName) {
        log.debug("Generating converter performance report: {}", converterName);
        
        return Mono.zip(
                converterMetricsRepository.getTotalExecutions(converterName),
                converterMetricsRepository.getTotalSuccesses(converterName),
                converterMetricsRepository.getTotalFailures(converterName),
                converterMetricsRepository.calculateSuccessRate(converterName)
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            report.put("converter_name", converterName);
            report.put("total_executions", tuple.getT1());
            report.put("total_successes", tuple.getT2());
            report.put("total_failures", tuple.getT3());
            report.put("success_rate_percentage", tuple.getT4());
            report.put("generated_at", Instant.now());
            
            log.info("Converter performance report generated: converter={}, executions={}, successRate={}", 
                    converterName, tuple.getT1(), tuple.getT4());
            return report;
        });
    }

    /**
     * Generate payment status distribution report
     */
    public Mono<Map<String, Object>> generatePaymentStatusReport() {
        log.debug("Generating payment status distribution report");
        
        return Mono.zip(
                paymentStatusReportRepository.countByStatusCode(PaymentStatusCode.ACCP),
                paymentStatusReportRepository.countByStatusCode(PaymentStatusCode.ACSC),
                paymentStatusReportRepository.countByStatusCode(PaymentStatusCode.RJCT),
                paymentStatusReportRepository.countByStatusCode(PaymentStatusCode.PDNG),
                paymentStatusReportRepository.countByStatusCode(PaymentStatusCode.PART)
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            report.put("accepted", tuple.getT1());
            report.put("settled", tuple.getT2());
            report.put("rejected", tuple.getT3());
            report.put("pending", tuple.getT4());
            report.put("partial", tuple.getT5());
            
            long total = tuple.getT1() + tuple.getT2() + tuple.getT3() + tuple.getT4() + tuple.getT5();
            report.put("total", total);
            
            if (total > 0) {
                report.put("acceptance_rate", (tuple.getT1() + tuple.getT2()) * 100.0 / total);
                report.put("rejection_rate", tuple.getT3() * 100.0 / total);
            } else {
                report.put("acceptance_rate", 0.0);
                report.put("rejection_rate", 0.0);
            }
            
            report.put("generated_at", Instant.now());
            
            log.info("Payment status report generated: total={}, acceptanceRate={}", 
                    total, report.get("acceptance_rate"));
            return report;
        });
    }

    /**
     * Generate account activity report
     */
    public Mono<Map<String, Object>> generateAccountActivityReport(UUID accountId, LocalDate startDate, LocalDate endDate) {
        log.debug("Generating account activity report: accountId={}, range={} to {}", 
                accountId, startDate, endDate);
        
        return Mono.zip(
                accountTransactionRepository.calculateTotalDebits(accountId, startDate, endDate),
                accountTransactionRepository.calculateTotalCredits(accountId, startDate, endDate),
                accountTransactionRepository.findRecentTransactions(accountId, 100).count()
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            report.put("account_id", accountId);
            report.put("start_date", startDate);
            report.put("end_date", endDate);
            report.put("total_debits", tuple.getT1());
            report.put("total_credits", tuple.getT2());
            report.put("net_amount", tuple.getT2() - tuple.getT1());
            report.put("transaction_count", tuple.getT3());
            report.put("generated_at", Instant.now());
            
            log.info("Account activity report generated: accountId={}, debits={}, credits={}, netAmount={}", 
                    accountId, tuple.getT1(), tuple.getT2(), report.get("net_amount"));
            return report;
        });
    }

    /**
     * Generate validation error summary report
     */
    public Mono<Map<String, Object>> generateValidationErrorReport() {
        log.debug("Generating validation error summary report");
        
        return Mono.zip(
                validationErrorRepository.countBySeverity("INFO"),
                validationErrorRepository.countBySeverity("WARNING"),
                validationErrorRepository.countBySeverity("ERROR"),
                validationErrorRepository.countBySeverity("CRITICAL")
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            report.put("info_count", tuple.getT1());
            report.put("warning_count", tuple.getT2());
            report.put("error_count", tuple.getT3());
            report.put("critical_count", tuple.getT4());
            
            long total = tuple.getT1() + tuple.getT2() + tuple.getT3() + tuple.getT4();
            report.put("total_errors", total);
            report.put("generated_at", Instant.now());
            
            log.info("Validation error report generated: total={}, critical={}", total, tuple.getT4());
            return report;
        });
    }

    /**
     * Generate conversion analytics report
     */
    public Mono<Map<String, Object>> generateConversionAnalyticsReport() {
        log.debug("Generating conversion analytics report");
        
        return Mono.zip(
                conversionRepository.findRecentConversions(1000).count(),
                conversionRepository.findFailedConversions(1000).count()
        ).map(tuple -> {
            Map<String, Object> report = new HashMap<>();
            long totalConversions = tuple.getT1();
            long failedConversions = tuple.getT2();
            long successfulConversions = totalConversions - failedConversions;
            
            report.put("total_conversions", totalConversions);
            report.put("successful_conversions", successfulConversions);
            report.put("failed_conversions", failedConversions);
            
            if (totalConversions > 0) {
                report.put("success_rate", successfulConversions * 100.0 / totalConversions);
                report.put("failure_rate", failedConversions * 100.0 / totalConversions);
            } else {
                report.put("success_rate", 0.0);
                report.put("failure_rate", 0.0);
            }
            
            report.put("generated_at", Instant.now());
            
            log.info("Conversion analytics report generated: total={}, successRate={}", 
                    totalConversions, report.get("success_rate"));
            return report;
        });
    }

    /**
     * Generate comprehensive dashboard report
     */
    public Mono<Map<String, Object>> generateDashboardReport() {
        log.debug("Generating comprehensive dashboard report");
        
        return Mono.zip(
                generateSystemHealthReport(),
                generatePaymentStatusReport(),
                generateValidationErrorReport(),
                generateConversionAnalyticsReport()
        ).map(tuple -> {
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("system_health", tuple.getT1());
            dashboard.put("payment_status", tuple.getT2());
            dashboard.put("validation_errors", tuple.getT3());
            dashboard.put("conversion_analytics", tuple.getT4());
            dashboard.put("generated_at", Instant.now());
            
            log.info("Dashboard report generated");
            return dashboard;
        });
    }
}
