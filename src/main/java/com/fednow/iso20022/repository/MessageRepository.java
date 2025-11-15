package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Message;
import com.fednow.iso20022.entity.enums.MessageStatus;
import com.fednow.iso20022.entity.enums.MessageType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Message entities
 */
@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, UUID> {

    /**
     * Find message by message ID
     */
    Mono<Message> findByMessageId(String messageId);

    /**
     * Find messages by UETR
     */
    Flux<Message> findByUetr(UUID uetr);

    /**
     * Find messages by end-to-end ID
     */
    Flux<Message> findByEndToEndId(String endToEndId);

    /**
     * Find messages by transaction ID
     */
    Flux<Message> findByTransactionId(String transactionId);

    /**
     * Find messages by status
     */
    Flux<Message> findByStatus(MessageStatus status);

    /**
     * Find messages by message type
     */
    Flux<Message> findByMessageType(MessageType messageType);

    /**
     * Find messages by status and message type
     */
    Flux<Message> findByStatusAndMessageType(MessageStatus status, MessageType messageType);

    /**
     * Find recent messages (last 100)
     */
    @Query("SELECT * FROM messages ORDER BY created_at DESC LIMIT 100")
    Flux<Message> findRecentMessages();

    /**
     * Count messages by status
     */
    Mono<Long> countByStatus(MessageStatus status);

    /**
     * Find messages by original message ID
     */
    Flux<Message> findByOriginalMessageId(String originalMessageId);
}
