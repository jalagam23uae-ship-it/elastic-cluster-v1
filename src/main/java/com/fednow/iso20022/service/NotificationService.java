package com.fednow.iso20022.service;

import com.fednow.iso20022.entity.SystemEvent;
import com.fednow.iso20022.repository.SystemEventRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for system event notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SystemEventRepository systemEventRepository;

    /**
     * Record a system event
     */
    public Mono<SystemEvent> recordSystemEvent(
            String eventCode,
            String eventDescription,
            String severity,
            UUID relatedMessageId,
            String eventParametersJson,
            String additionalInfo) {
        
        log.debug("Recording system event: code={}, severity={}, relatedMessageId={}", 
                eventCode, severity, relatedMessageId);
        
        SystemEvent event = new SystemEvent();
        event.setEventCode(eventCode);
        event.setEventDescription(eventDescription);
        event.setSeverity(severity);
        event.setRelatedMessageId(relatedMessageId);
        event.setEventTime(Instant.now());
        if (eventParametersJson != null) {
            event.setEventParameters(Json.of(eventParametersJson));
        }
        event.setAdditionalInfo(additionalInfo);
        
        return systemEventRepository.save(event)
                .doOnSuccess(saved -> {
                    if ("FATAL".equals(severity) || "ERROR".equals(severity)) {
                        log.error("System event recorded: id={}, code={}, severity={}, description={}", 
                                saved.getId(), eventCode, severity, eventDescription);
                    } else {
                        log.info("System event recorded: id={}, code={}, severity={}, description={}", 
                                saved.getId(), eventCode, severity, eventDescription);
                    }
                })
                .doOnError(e -> log.error("Failed to record system event: code={}, severity={}", 
                        eventCode, severity, e));
    }

    /**
     * Record an info event
     */
    public Mono<SystemEvent> recordInfoEvent(String eventCode, String eventDescription, UUID relatedMessageId) {
        return recordSystemEvent(eventCode, eventDescription, "INFO", relatedMessageId, null, null);
    }

    /**
     * Record a warning event
     */
    public Mono<SystemEvent> recordWarningEvent(String eventCode, String eventDescription, UUID relatedMessageId) {
        return recordSystemEvent(eventCode, eventDescription, "WARNING", relatedMessageId, null, null);
    }

    /**
     * Record an error event
     */
    public Mono<SystemEvent> recordErrorEvent(String eventCode, String eventDescription, UUID relatedMessageId, String additionalInfo) {
        return recordSystemEvent(eventCode, eventDescription, "ERROR", relatedMessageId, null, additionalInfo);
    }

    /**
     * Record a fatal event
     */
    public Mono<SystemEvent> recordFatalEvent(String eventCode, String eventDescription, String additionalInfo) {
        return recordSystemEvent(eventCode, eventDescription, "FATAL", null, null, additionalInfo);
    }

    /**
     * Get recent system events
     */
    public Flux<SystemEvent> getRecentEvents(int limit) {
        log.debug("Retrieving recent system events: limit={}", limit);
        return systemEventRepository.findRecentEvents(limit);
    }

    /**
     * Get error events
     */
    public Flux<SystemEvent> getErrorEvents(int limit) {
        log.debug("Retrieving error events: limit={}", limit);
        return systemEventRepository.findErrorEvents(limit);
    }

    /**
     * Get fatal events
     */
    public Flux<SystemEvent> getFatalEvents() {
        log.debug("Retrieving fatal events");
        return systemEventRepository.findFatalEvents();
    }

    /**
     * Get events by event code
     */
    public Flux<SystemEvent> getEventsByCode(String eventCode) {
        log.debug("Retrieving events by code: {}", eventCode);
        return systemEventRepository.findByEventCode(eventCode);
    }

    /**
     * Get events by severity
     */
    public Flux<SystemEvent> getEventsBySeverity(String severity) {
        log.debug("Retrieving events by severity: {}", severity);
        return systemEventRepository.findBySeverity(severity);
    }

    /**
     * Get events for a specific message
     */
    public Flux<SystemEvent> getEventsForMessage(UUID messageId) {
        log.debug("Retrieving events for message: {}", messageId);
        return systemEventRepository.findByRelatedMessageId(messageId);
    }

    /**
     * Get events in time range
     */
    public Flux<SystemEvent> getEventsInTimeRange(Instant startTime, Instant endTime) {
        log.debug("Retrieving events in time range: {} to {}", startTime, endTime);
        return systemEventRepository.findByEventTimeBetween(startTime, endTime);
    }

    /**
     * Count events by severity
     */
    public Mono<Long> countEventsBySeverity(String severity) {
        log.debug("Counting events by severity: {}", severity);
        return systemEventRepository.countBySeverity(severity);
    }
}
