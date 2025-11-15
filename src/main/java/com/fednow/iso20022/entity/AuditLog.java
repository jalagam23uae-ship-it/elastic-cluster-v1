package com.fednow.iso20022.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * AuditLog entity - audit trail for all message operations
 */
@Table("audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @Column("message_id")
    private UUID messageId;

    @Column("action")
    private String action;

    @Column("actor")
    private String actor;

    @Column("details")
    private Json details;

    @Column("timestamp")
    private Instant timestamp;

    // Constructors
    public AuditLog() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    
    public Json getDetails() { return details; }
    public void setDetails(Json details) { this.details = details; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
