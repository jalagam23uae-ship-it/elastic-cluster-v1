package com.fednow.iso20022.service;

import com.fednow.iso20022.entity.AuditLog;
import com.fednow.iso20022.entity.Message;
import com.fednow.iso20022.entity.enums.MessageStatus;
import com.fednow.iso20022.repository.AuditLogRepository;
import com.fednow.iso20022.repository.MessageRepository;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for message lifecycle orchestration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingService {

    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Save a new message and create audit log entry
     */
    public Mono<Message> saveMessage(Message message) {
        log.debug("Saving message: messageId={}, type={}", message.getMessageId(), message.getMessageType());
        
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(Instant.now());
        }
        if (message.getUpdatedAt() == null) {
            message.setUpdatedAt(Instant.now());
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.RECEIVED);
        }
        
        return messageRepository.save(message)
                .flatMap(saved -> {
                    log.info("Message saved: id={}, messageId={}, type={}", 
                            saved.getId(), saved.getMessageId(), saved.getMessageType());
                    return createAuditLog(saved.getId(), "MESSAGE_CREATED", "SYSTEM",
                            String.format("{\"message_type\":\"%s\",\"message_id\":\"%s\",\"status\":\"%s\"}", 
                                    saved.getMessageType(), saved.getMessageId(), saved.getStatus()))
                            .thenReturn(saved);
                })
                .doOnError(e -> log.error("Failed to save message: messageId={}", message.getMessageId(), e));
    }

    /**
     * Update message status
     */
    public Mono<Message> updateMessageStatus(UUID messageId, MessageStatus newStatus) {
        log.debug("Updating message status: id={}, newStatus={}", messageId, newStatus);
        
        return messageRepository.findById(messageId)
                .flatMap(message -> {
                    MessageStatus oldStatus = message.getStatus();
                    message.setStatus(newStatus);
                    message.setUpdatedAt(Instant.now());
                    
                    if (newStatus == MessageStatus.CONVERTED || newStatus == MessageStatus.SENT) {
                        message.setProcessedAt(Instant.now());
                    }
                    
                    return messageRepository.save(message)
                            .flatMap(saved -> {
                                log.info("Message status updated: id={}, oldStatus={}, newStatus={}", 
                                        messageId, oldStatus, newStatus);
                                return createAuditLog(messageId, "STATUS_CHANGED", "SYSTEM",
                                        String.format("{\"old_status\":\"%s\",\"new_status\":\"%s\"}", 
                                                oldStatus, newStatus))
                                        .thenReturn(saved);
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Message not found: " + messageId)))
                .doOnError(e -> log.error("Failed to update message status: id={}, newStatus={}", 
                        messageId, newStatus, e));
    }

    /**
     * Get message by message ID
     */
    public Mono<Message> getMessageByMessageId(String messageId) {
        log.debug("Retrieving message by messageId: {}", messageId);
        return messageRepository.findByMessageId(messageId);
    }

    /**
     * Get messages by UETR
     */
    public Flux<Message> getMessagesByUetr(UUID uetr) {
        log.debug("Retrieving messages by UETR: {}", uetr);
        return messageRepository.findByUetr(uetr);
    }

    /**
     * Get messages by end-to-end ID
     */
    public Flux<Message> getMessagesByEndToEndId(String endToEndId) {
        log.debug("Retrieving messages by end-to-end ID: {}", endToEndId);
        return messageRepository.findByEndToEndId(endToEndId);
    }

    /**
     * Get recent messages
     */
    public Flux<Message> getRecentMessages() {
        log.debug("Retrieving recent messages");
        return messageRepository.findRecentMessages();
    }

    /**
     * Get messages by status
     */
    public Flux<Message> getMessagesByStatus(MessageStatus status) {
        log.debug("Retrieving messages by status: {}", status);
        return messageRepository.findByStatus(status);
    }

    /**
     * Get complete audit trail for a message
     */
    public Flux<AuditLog> getMessageAuditTrail(UUID messageId) {
        log.debug("Retrieving audit trail for message: {}", messageId);
        return auditLogRepository.findMessageAuditTrail(messageId);
    }

    /**
     * Create audit log entry
     */
    private Mono<AuditLog> createAuditLog(UUID messageId, String action, String actor, String detailsJson) {
        AuditLog auditLog = new AuditLog();
        auditLog.setMessageId(messageId);
        auditLog.setAction(action);
        auditLog.setActor(actor);
        auditLog.setDetails(Json.of(detailsJson));
        auditLog.setTimestamp(Instant.now());
        
        return auditLogRepository.save(auditLog)
                .doOnSuccess(saved -> log.debug("Audit log created: id={}, action={}, messageId={}", 
                        saved.getId(), action, messageId))
                .doOnError(e -> log.error("Failed to create audit log: messageId={}, action={}", 
                        messageId, action, e));
    }

    /**
     * Count messages by status
     */
    public Mono<Long> countMessagesByStatus(MessageStatus status) {
        log.debug("Counting messages by status: {}", status);
        return messageRepository.countByStatus(status);
    }
}
