package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.entity.SystemEvent;
import com.fednow.iso20022.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST API Controller for System Event Monitoring and Notifications.
 *
 * Provides endpoints for:
 * - Querying system events by severity, code, time range
 * - Retrieving events for specific messages
 * - Monitoring error and fatal events
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "System Events", description = "System event monitoring and notifications")
public class SystemEventsController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get recent system events",
            description = """
                    Retrieves recent system events ordered by timestamp (most recent first):
                    - Event code, description, severity
                    - Related message ID
                    - Event parameters and additional info

                    **Use Case:** Monitor recent system activity
                    """
    )
    @GetMapping(value = "/recent", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getRecentEvents(
            @Parameter(description = "Maximum number of events to retrieve (default: 100)")
            @RequestParam(defaultValue = "100") int limit) {

        log.info("Retrieving recent system events: limit={}", limit);

        return notificationService.getRecentEvents(limit)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Retrieved %d recent events", events.size())))
                .doOnSuccess(response -> log.info("Retrieved {} recent system events", response.getData().size()))
                .doOnError(e -> log.error("Failed to retrieve recent events", e));
    }

    @Operation(
            summary = "Get error events",
            description = """
                    Retrieves ERROR and FATAL severity events:
                    - Critical system errors requiring attention
                    - Fatal events that caused system failures

                    **Use Case:** Monitor system errors for alerting and debugging
                    """
    )
    @GetMapping(value = "/errors", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getErrorEvents(
            @Parameter(description = "Maximum number of error events to retrieve (default: 50)")
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Retrieving error events: limit={}", limit);

        return notificationService.getErrorEvents(limit)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Retrieved %d error events", events.size())))
                .doOnSuccess(response -> {
                    if (!response.getData().isEmpty()) {
                        log.warn("Found {} error/fatal events", response.getData().size());
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve error events", e));
    }

    @Operation(
            summary = "Get fatal events",
            description = """
                    Retrieves only FATAL severity events:
                    - Critical system failures
                    - Events that caused immediate system shutdown or severe issues

                    **Use Case:** Monitor critical system failures
                    """
    )
    @GetMapping(value = "/fatal", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getFatalEvents() {
        log.info("Retrieving fatal events");

        return notificationService.getFatalEvents()
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Retrieved %d fatal events", events.size())))
                .doOnSuccess(response -> {
                    if (!response.getData().isEmpty()) {
                        log.error("Found {} FATAL system events", response.getData().size());
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve fatal events", e));
    }

    @Operation(
            summary = "Get events by event code",
            description = """
                    Retrieves events by specific event code:
                    - Filter by event type (e.g., VALIDATION_FAILED, CONVERSION_TIMEOUT)
                    - All occurrences across all time

                    **Use Case:** Track specific event types
                    """
    )
    @GetMapping(value = "/code/{eventCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getEventsByCode(
            @Parameter(description = "Event code (e.g., VALIDATION_FAILED, CONVERSION_TIMEOUT)", required = true)
            @PathVariable String eventCode) {

        log.info("Retrieving events by code: {}", eventCode);

        return notificationService.getEventsByCode(eventCode)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Found %d events with code %s", events.size(), eventCode)))
                .doOnSuccess(response -> log.info("Found {} events with code: {}", response.getData().size(), eventCode))
                .doOnError(e -> log.error("Failed to retrieve events by code: {}", eventCode, e));
    }

    @Operation(
            summary = "Get events by severity",
            description = """
                    Retrieves events by severity level:
                    - Severity levels: INFO, WARNING, ERROR, FATAL
                    - Filtered across all events

                    **Use Case:** Monitor events by severity for alerting
                    """
    )
    @GetMapping(value = "/severity/{severity}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getEventsBySeverity(
            @Parameter(description = "Severity (INFO, WARNING, ERROR, FATAL)", required = true)
            @PathVariable String severity) {

        log.info("Retrieving events by severity: {}", severity);

        return notificationService.getEventsBySeverity(severity)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Found %d events with severity %s", events.size(), severity)))
                .doOnSuccess(response -> log.info("Found {} events with severity: {}",
                        response.getData().size(), severity))
                .doOnError(e -> log.error("Failed to retrieve events by severity: {}", severity, e));
    }

    @Operation(
            summary = "Get events for a specific message",
            description = """
                    Retrieves all events related to a specific message:
                    - Validation failures
                    - Conversion errors
                    - Processing events

                    **Use Case:** Debug message processing issues
                    """
    )
    @GetMapping(value = "/message/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getEventsForMessage(
            @Parameter(description = "Message ID", required = true)
            @PathVariable UUID messageId) {

        log.info("Retrieving events for message: {}", messageId);

        return notificationService.getEventsForMessage(messageId)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Found %d events for message %s", events.size(), messageId)))
                .doOnSuccess(response -> log.info("Found {} events for message: {}",
                        response.getData().size(), messageId))
                .doOnError(e -> log.error("Failed to retrieve events for message: {}", messageId, e));
    }

    @Operation(
            summary = "Get events in time range",
            description = """
                    Retrieves events within a specific time range:
                    - Start and end timestamps
                    - All events occurring within the range

                    **Use Case:** Analyze events during specific time periods
                    """
    )
    @GetMapping(value = "/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<SystemEvent>>> getEventsInTimeRange(
            @Parameter(description = "Start time (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time (ISO 8601 format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.info("Retrieving events in time range: {} to {}", startTime, endTime);

        return notificationService.getEventsInTimeRange(startTime, endTime)
                .collectList()
                .map(events -> ApiResponse.success(events,
                        String.format("Found %d events from %s to %s", events.size(), startTime, endTime)))
                .doOnSuccess(response -> log.info("Found {} events in time range", response.getData().size()))
                .doOnError(e -> log.error("Failed to retrieve events in time range", e));
    }

    @Operation(
            summary = "Count events by severity",
            description = """
                    Counts events by severity level:
                    - Returns count of events with specified severity
                    - Useful for monitoring and alerting thresholds

                    **Use Case:** Monitor event counts for alerting rules
                    """
    )
    @GetMapping(value = "/count/severity/{severity}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Long>> countEventsBySeverity(
            @Parameter(description = "Severity (INFO, WARNING, ERROR, FATAL)", required = true)
            @PathVariable String severity) {

        log.info("Counting events by severity: {}", severity);

        return notificationService.countEventsBySeverity(severity)
                .map(count -> ApiResponse.success(count,
                        String.format("Count of events with severity %s: %d", severity, count)))
                .doOnSuccess(response -> log.info("{} events with severity: {}", response.getData(), severity))
                .doOnError(e -> log.error("Failed to count events by severity: {}", severity, e));
    }

    @Operation(
            summary = "Record an info event",
            description = """
                    Records an informational system event:
                    - INFO severity level
                    - Can be related to a specific message

                    **Use Case:** Record system informational events
                    """
    )
    @PostMapping(value = "/info", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<SystemEvent>> recordInfoEvent(
            @Parameter(description = "Info event details", required = true)
            @RequestBody EventRequest request) {

        log.info("Recording info event: code={}, messageId={}", request.eventCode, request.relatedMessageId);

        return notificationService.recordInfoEvent(request.eventCode, request.eventDescription, request.relatedMessageId)
                .map(event -> ApiResponse.success(event,
                        String.format("Info event recorded: %s", event.getEventCode())))
                .doOnSuccess(response -> log.info("Info event recorded: id={}, code={}",
                        response.getData().getId(), response.getData().getEventCode()))
                .doOnError(e -> log.error("Failed to record info event", e));
    }

    @Operation(
            summary = "Record a warning event",
            description = """
                    Records a warning system event:
                    - WARNING severity level
                    - Indicates potential issues requiring attention

                    **Use Case:** Record system warnings
                    """
    )
    @PostMapping(value = "/warning", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<SystemEvent>> recordWarningEvent(
            @Parameter(description = "Warning event details", required = true)
            @RequestBody EventRequest request) {

        log.info("Recording warning event: code={}, messageId={}", request.eventCode, request.relatedMessageId);

        return notificationService.recordWarningEvent(request.eventCode, request.eventDescription, request.relatedMessageId)
                .map(event -> ApiResponse.success(event,
                        String.format("Warning event recorded: %s", event.getEventCode())))
                .doOnSuccess(response -> log.warn("Warning event recorded: id={}, code={}",
                        response.getData().getId(), response.getData().getEventCode()))
                .doOnError(e -> log.error("Failed to record warning event", e));
    }

    @Operation(
            summary = "Record an error event",
            description = """
                    Records an error system event:
                    - ERROR severity level
                    - Indicates errors requiring immediate attention
                    - Can include additional error information

                    **Use Case:** Record system errors
                    """
    )
    @PostMapping(value = "/error", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<SystemEvent>> recordErrorEvent(
            @Parameter(description = "Error event details", required = true)
            @RequestBody ErrorEventRequest request) {

        log.info("Recording error event: code={}, messageId={}", request.eventCode, request.relatedMessageId);

        return notificationService.recordErrorEvent(
                        request.eventCode,
                        request.eventDescription,
                        request.relatedMessageId,
                        request.additionalInfo
                )
                .map(event -> ApiResponse.success(event,
                        String.format("Error event recorded: %s", event.getEventCode())))
                .doOnSuccess(response -> log.error("Error event recorded: id={}, code={}",
                        response.getData().getId(), response.getData().getEventCode()))
                .doOnError(e -> log.error("Failed to record error event", e));
    }

    /**
     * Request DTO for basic events.
     */
    public record EventRequest(
            String eventCode,
            String eventDescription,
            UUID relatedMessageId
    ) {}

    /**
     * Request DTO for error events.
     */
    public record ErrorEventRequest(
            String eventCode,
            String eventDescription,
            UUID relatedMessageId,
            String additionalInfo
    ) {}
}
