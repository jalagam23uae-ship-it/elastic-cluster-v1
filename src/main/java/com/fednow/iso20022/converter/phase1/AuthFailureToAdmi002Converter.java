package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.admi.Admi002;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * AuthFailureToAdmi002Converter - Authentication and Security Event Handler
 *
 * Converts authentication/security failures to admi.002 (System Event Notification).
 *
 * This is a CRITICAL security converter for handling authentication, authorization,
 * and security-related failures in the FedNow payment system.
 *
 * Security Event Types:
 * 1. Authentication Failures (AUTHF):
 *    - Invalid credentials
 *    - Expired certificates
 *    - Signature verification failures
 *    - Invalid API keys/tokens
 *    - Multi-factor authentication failures
 *
 * 2. Encryption Failures (ENCF):
 *    - TLS handshake failures
 *    - Message encryption/decryption errors
 *    - Invalid encryption keys
 *    - Cipher mismatch
 *
 * 3. Authorization Failures (AUTZ):
 *    - Insufficient permissions
 *    - Access denied
 *    - Role/privilege violations
 *    - IP address restrictions
 *
 * 4. Security Violations (SECV):
 *    - Suspicious activity detected
 *    - Rate limiting exceeded
 *    - Potential intrusion attempts
 *    - Invalid message signatures
 *
 * FedNow Security Requirements:
 * - All authentication failures must be logged
 * - Must respond within 1 second
 * - Security events trigger monitoring alerts
 * - Multiple failures may trigger account lockout
 * - All security events must be audited
 *
 * Compliance:
 * - PCI DSS requirements for authentication
 * - Federal banking security standards
 * - FedNow participant security guidelines
 */
@Slf4j
@Component
public class AuthFailureToAdmi002Converter extends AbstractMessageConverter<Object, Admi002> {

    public AuthFailureToAdmi002Converter() {
        super(Object.class, Admi002.class, "AuthFailureToAdmi002Converter");
    }

    @Override
    protected Mono<Admi002> doConvert(Object source, ConverterContext context) {
        logStep("Starting AUTH_FAILURE → admi.002 conversion (security event)");

        // Generate message ID for admi.002
        enrichContextWithIds(context, "SECERR", false, false);

        return Mono.fromCallable(() -> {
            // Get security event details from context
            String securityEventType = context.getAttribute("securityEventType", String.class);
            String failureReason = context.getAttribute("failureReason", String.class);
            String affectedComponent = context.getAttribute("affectedComponent", String.class);
            String username = context.getAttribute("username", String.class);
            String ipAddress = context.getAttribute("ipAddress", String.class);
            Integer failureCount = context.getAttribute("failureCount", Integer.class);

            // Default values
            if (securityEventType == null) securityEventType = "AUTHF";
            if (failureReason == null) failureReason = "Authentication failed";
            if (affectedComponent == null) affectedComponent = "Authentication";

            // Build event reason
            Admi002.EventReason reason = buildSecurityEventReason(
                    securityEventType,
                    failureReason,
                    affectedComponent,
                    username,
                    ipAddress,
                    failureCount);

            // Build admi.002
            Admi002 admi002 = Admi002.builder()
                    .messageId(context.getGeneratedMessageId())
                    .creationDateTime(context.getCurrentTimestamp())
                    .relatedReference(buildRelatedReference(source, context))
                    .reason(reason)
                    .originalBusinessInstruction(buildOriginalBusinessInstruction(source, context))
                    .build();

            logStep("Successfully created admi.002 security event notification: " + securityEventType);

            // Log security event for audit trail
            logSecurityEvent(securityEventType, failureReason, username, ipAddress);

            return admi002;
        });
    }

    /**
     * Builds security event reason.
     *
     * @param eventType security event type
     * @param failureReason failure reason
     * @param component affected component
     * @param username username if available
     * @param ipAddress IP address if available
     * @param failureCount number of consecutive failures
     * @return event reason
     */
    private Admi002.EventReason buildSecurityEventReason(
            String eventType,
            String failureReason,
            String component,
            String username,
            String ipAddress,
            Integer failureCount) {

        List<String> additionalInfo = new ArrayList<>();
        additionalInfo.add("Security event: " + failureReason);

        if (username != null) {
            additionalInfo.add("User: " + username);
        }
        if (ipAddress != null) {
            additionalInfo.add("IP Address: " + ipAddress);
        }
        if (failureCount != null && failureCount > 1) {
            additionalInfo.add("Consecutive failures: " + failureCount);
            if (failureCount >= 3) {
                additionalInfo.add("WARNING: Multiple failed attempts detected");
            }
        }

        String recommendedAction = determineSecurityRecommendedAction(eventType, failureCount);
        String severity = determineSecuritySeverity(eventType, failureCount);

        return Admi002.EventReason.builder()
                .code(eventType)
                .severity(severity)
                .affectedComponent(component)
                .additionalInformation(additionalInfo)
                .recommendedAction(recommendedAction)
                .build();
    }

    /**
     * Determines recommended action for security event.
     *
     * @param eventType security event type
     * @param failureCount number of failures
     * @return recommended action
     */
    private String determineSecurityRecommendedAction(String eventType, Integer failureCount) {
        switch (eventType) {
            case "AUTHF":
                if (failureCount != null && failureCount >= 5) {
                    return "Account locked due to excessive failures - contact administrator";
                } else if (failureCount != null && failureCount >= 3) {
                    return "Verify credentials carefully - account will be locked after 5 attempts";
                }
                return "Verify credentials and retry with valid authentication";

            case "ENCF":
                return "Check TLS configuration and certificate validity";

            case "AUTZ":
                return "Contact administrator to verify permissions and access rights";

            case "SECV":
                if (failureCount != null && failureCount >= 3) {
                    return "Suspicious activity detected - account temporarily restricted";
                }
                return "Verify request authenticity and retry";

            case "CERT":
                return "Renew or update certificates and retry";

            case "SIGN":
                return "Verify message signature and signing keys";

            default:
                return "Contact security administrator for assistance";
        }
    }

    /**
     * Determines severity for security event.
     *
     * @param eventType security event type
     * @param failureCount number of failures
     * @return severity level
     */
    private String determineSecuritySeverity(String eventType, Integer failureCount) {
        // Escalate severity based on failure count
        if (failureCount != null && failureCount >= 5) {
            return "FATAL"; // Critical - likely attack
        } else if (failureCount != null && failureCount >= 3) {
            return "ERROR"; // High concern
        }

        switch (eventType) {
            case "AUTHF":
            case "ENCF":
                return "FATAL";
            case "AUTZ":
            case "SECV":
                return "ERROR";
            default:
                return "WARNING";
        }
    }

    /**
     * Logs security event for audit trail.
     *
     * @param eventType security event type
     * @param reason failure reason
     * @param username username
     * @param ipAddress IP address
     */
    private void logSecurityEvent(
            String eventType,
            String reason,
            String username,
            String ipAddress) {

        String logMessage = String.format(
                "SECURITY_EVENT [%s] - %s | User: %s | IP: %s",
                eventType,
                reason,
                username != null ? username : "UNKNOWN",
                ipAddress != null ? ipAddress : "UNKNOWN"
        );

        // Use different log levels based on severity
        if ("AUTHF".equals(eventType) || "ENCF".equals(eventType)) {
            log.error(logMessage);
        } else if ("AUTZ".equals(eventType) || "SECV".equals(eventType)) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }

    /**
     * Builds related reference from source message.
     */
    private Admi002.RelatedReference buildRelatedReference(Object source, ConverterContext context) {
        return Admi002.RelatedReference.builder()
                .messageId(extractMessageId(source))
                .messageNameIdentification(extractMessageType(source))
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds original business instruction.
     */
    private Admi002.OriginalBusinessInstruction buildOriginalBusinessInstruction(
            Object source, ConverterContext context) {

        String messageId = extractMessageId(source);
        String messageType = extractMessageType(source);

        if (messageId == null || messageType == null) {
            return null;
        }

        return Admi002.OriginalBusinessInstruction.builder()
                .messageId(messageId)
                .messageNameIdentification(messageType)
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Extracts message ID from source.
     */
    private String extractMessageId(Object source) {
        if (source == null) return "UNKNOWN";

        try {
            var groupHeaderMethod = source.getClass().getMethod("getGroupHeader");
            var groupHeader = groupHeaderMethod.invoke(source);
            if (groupHeader != null) {
                var messageIdMethod = groupHeader.getClass().getMethod("getMessageId");
                return (String) messageIdMethod.invoke(groupHeader);
            }
        } catch (Exception e) {
            // Ignore
        }

        return "UNKNOWN";
    }

    /**
     * Extracts message type from source.
     */
    private String extractMessageType(Object source) {
        if (source == null) return "unknown";
        String className = source.getClass().getSimpleName();
        if (className.matches("^[A-Z][a-z]+\\d{3}$")) {
            String prefix = className.substring(0, 4).toLowerCase();
            String suffix = className.substring(4);
            return prefix + "." + suffix + ".001.11";
        }
        return className.toLowerCase();
    }

    // ==================== Helper Methods for Common Security Events ====================

    /**
     * Creates admi.002 for invalid credentials.
     */
    public Mono<Admi002> createInvalidCredentials(
            Object source,
            ConverterContext context,
            String username,
            String ipAddress,
            int failureCount) {
        context.setAttribute("securityEventType", "AUTHF");
        context.setAttribute("failureReason", "Invalid credentials provided");
        context.setAttribute("affectedComponent", "Authentication");
        context.setAttribute("username", username);
        context.setAttribute("ipAddress", ipAddress);
        context.setAttribute("failureCount", failureCount);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for expired certificate.
     */
    public Mono<Admi002> createExpiredCertificate(
            Object source,
            ConverterContext context,
            String certificateSubject) {
        context.setAttribute("securityEventType", "CERT");
        context.setAttribute("failureReason", "Certificate expired: " + certificateSubject);
        context.setAttribute("affectedComponent", "Certificate Management");
        return convert(source, context);
    }

    /**
     * Creates admi.002 for signature verification failure.
     */
    public Mono<Admi002> createSignatureFailure(
            Object source,
            ConverterContext context,
            String details) {
        context.setAttribute("securityEventType", "SIGN");
        context.setAttribute("failureReason", "Message signature verification failed: " + details);
        context.setAttribute("affectedComponent", "Digital Signature");
        return convert(source, context);
    }

    /**
     * Creates admi.002 for TLS/encryption failure.
     */
    public Mono<Admi002> createEncryptionFailure(
            Object source,
            ConverterContext context,
            String details) {
        context.setAttribute("securityEventType", "ENCF");
        context.setAttribute("failureReason", "Encryption/TLS failure: " + details);
        context.setAttribute("affectedComponent", "Encryption");
        return convert(source, context);
    }

    /**
     * Creates admi.002 for authorization failure.
     */
    public Mono<Admi002> createAuthorizationFailure(
            Object source,
            ConverterContext context,
            String username,
            String requiredPermission) {
        context.setAttribute("securityEventType", "AUTZ");
        context.setAttribute("failureReason", "Insufficient permissions for " + requiredPermission);
        context.setAttribute("affectedComponent", "Authorization");
        context.setAttribute("username", username);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for suspicious activity.
     */
    public Mono<Admi002> createSuspiciousActivity(
            Object source,
            ConverterContext context,
            String username,
            String ipAddress,
            String activityDetails) {
        context.setAttribute("securityEventType", "SECV");
        context.setAttribute("failureReason", "Suspicious activity: " + activityDetails);
        context.setAttribute("affectedComponent", "Security Monitoring");
        context.setAttribute("username", username);
        context.setAttribute("ipAddress", ipAddress);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for rate limiting.
     */
    public Mono<Admi002> createRateLimitExceeded(
            Object source,
            ConverterContext context,
            String username,
            String ipAddress,
            int requestCount,
            int limit) {
        context.setAttribute("securityEventType", "SECV");
        context.setAttribute("failureReason",
                "Rate limit exceeded: " + requestCount + " requests (limit: " + limit + ")");
        context.setAttribute("affectedComponent", "Rate Limiting");
        context.setAttribute("username", username);
        context.setAttribute("ipAddress", ipAddress);
        return convert(source, context);
    }
}
