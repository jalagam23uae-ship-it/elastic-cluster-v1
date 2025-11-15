package com.fednow.iso20022.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * SystemEvent entity - records system events and notifications (admi.002)
 */
@Table("system_events")
public class SystemEvent {

    @Id
    private UUID id;

    @Column("event_code")
    private String eventCode;

    @Column("event_description")
    private String eventDescription;

    @Column("severity")
    private String severity;  // INFO, WARNING, ERROR, FATAL

    @Column("related_message_id")
    private UUID relatedMessageId;

    @Column("event_time")
    private Instant eventTime;

    @Column("event_parameters")
    private Json eventParameters;

    @Column("additional_info")
    private String additionalInfo;

    // Constructors
    public SystemEvent() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    
    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public UUID getRelatedMessageId() { return relatedMessageId; }
    public void setRelatedMessageId(UUID relatedMessageId) { this.relatedMessageId = relatedMessageId; }
    
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    
    public Json getEventParameters() { return eventParameters; }
    public void setEventParameters(Json eventParameters) { this.eventParameters = eventParameters; }
    
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
