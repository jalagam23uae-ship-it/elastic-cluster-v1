package com.fednow.iso20022.service;

import com.fednow.iso20022.entity.ValidationError;
import com.fednow.iso20022.repository.ValidationErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for validation error recording and retrieval
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final ValidationErrorRepository validationErrorRepository;

    /**
     * Record a validation error
     */
    public Mono<ValidationError> recordValidationError(
            UUID messageId,
            String errorCode,
            String errorCategory,
            String severity,
            String fieldPath,
            String errorDescription) {
        
        log.debug("Recording validation error for message: {}, code: {}, severity: {}", 
                messageId, errorCode, severity);
        
        ValidationError error = new ValidationError();
        error.setMessageId(messageId);
        error.setErrorCode(errorCode);
        error.setErrorCategory(errorCategory);
        error.setSeverity(severity);
        error.setFieldPath(fieldPath);
        error.setErrorDescription(errorDescription);
        error.setCreatedAt(Instant.now());
        
        return validationErrorRepository.save(error)
                .doOnSuccess(saved -> log.info("Validation error recorded: id={}, messageId={}, code={}", 
                        saved.getId(), messageId, errorCode))
                .doOnError(e -> log.error("Failed to record validation error: messageId={}, code={}", 
                        messageId, errorCode, e));
    }

    /**
     * Record multiple validation errors
     */
    public Flux<ValidationError> recordValidationErrors(List<ValidationError> errors) {
        log.debug("Recording {} validation errors", errors.size());
        
        errors.forEach(error -> {
            if (error.getCreatedAt() == null) {
                error.setCreatedAt(Instant.now());
            }
        });
        
        return validationErrorRepository.saveAll(errors)
                .doOnComplete(() -> log.info("Recorded {} validation errors", errors.size()))
                .doOnError(e -> log.error("Failed to record validation errors", e));
    }

    /**
     * Get all validation errors for a message
     */
    public Flux<ValidationError> getValidationErrors(UUID messageId) {
        log.debug("Retrieving validation errors for message: {}", messageId);
        return validationErrorRepository.findByMessageId(messageId);
    }

    /**
     * Get critical validation errors for a message
     */
    public Flux<ValidationError> getCriticalErrors(UUID messageId) {
        log.debug("Retrieving critical validation errors for message: {}", messageId);
        return validationErrorRepository.findByMessageIdAndSeverity(messageId, "CRITICAL");
    }

    /**
     * Get all critical validation errors (system-wide)
     */
    public Flux<ValidationError> getAllCriticalErrors() {
        log.debug("Retrieving all critical validation errors");
        return validationErrorRepository.findCriticalErrors();
    }

    /**
     * Get validation error count for a message
     */
    public Mono<Long> getValidationErrorCount(UUID messageId) {
        log.debug("Counting validation errors for message: {}", messageId);
        return validationErrorRepository.countByMessageId(messageId);
    }

    /**
     * Check if message has critical errors
     */
    public Mono<Boolean> hasCriticalErrors(UUID messageId) {
        log.debug("Checking for critical errors in message: {}", messageId);
        return validationErrorRepository.findByMessageIdAndSeverity(messageId, "CRITICAL")
                .hasElements();
    }

    /**
     * Check if message passed validation (no errors)
     */
    public Mono<Boolean> isValid(UUID messageId) {
        log.debug("Checking if message is valid: {}", messageId);
        return validationErrorRepository.countByMessageId(messageId)
                .map(count -> count == 0);
    }

    /**
     * Get validation errors by severity
     */
    public Flux<ValidationError> getErrorsBySeverity(String severity) {
        log.debug("Retrieving validation errors by severity: {}", severity);
        return validationErrorRepository.findBySeverity(severity);
    }

    /**
     * Get validation errors by category
     */
    public Flux<ValidationError> getErrorsByCategory(String errorCategory) {
        log.debug("Retrieving validation errors by category: {}", errorCategory);
        return validationErrorRepository.findByErrorCategory(errorCategory);
    }
}
