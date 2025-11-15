package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Message;
import com.fednow.iso20022.entity.enums.MessageStatus;
import com.fednow.iso20022.entity.enums.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Integration tests for MessageRepository using Testcontainers
 */
@DataR2dbcTest
@Testcontainers
class MessageRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fednow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MessageRepository messageRepository;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        messageRepository.deleteAll().block();

        // Create test message
        testMessage = new Message();
        testMessage.setMessageId("MSG" + UUID.randomUUID().toString().substring(0, 8));
        testMessage.setMessageType(MessageType.PACS_008);
        testMessage.setUetr(UUID.randomUUID());
        testMessage.setEndToEndId("E2E123456");
        testMessage.setTransactionId("TXN123456");
        testMessage.setStatus(MessageStatus.RECEIVED);
        testMessage.setDirection("INBOUND");
        testMessage.setXmlContent("<xml>test</xml>");
        testMessage.setCreatedAt(Instant.now());
        testMessage.setUpdatedAt(Instant.now());
        testMessage.setDebtorName("John Doe");
        testMessage.setDebtorAccount("US12345678901234567890");
        testMessage.setCreditorName("Jane Smith");
        testMessage.setCreditorAccount("US98765432109876543210");
        testMessage.setAmount(new BigDecimal("1000.00"));
        testMessage.setCurrency("USD");
    }

    @Test
    void testSaveAndFindById() {
        StepVerifier.create(messageRepository.save(testMessage))
                .assertNext(saved -> {
                    assert saved.getId() != null;
                    assert saved.getMessageId().equals(testMessage.getMessageId());
                })
                .verifyComplete();
    }

    @Test
    void testFindByMessageId() {
        messageRepository.save(testMessage).block();

        StepVerifier.create(messageRepository.findByMessageId(testMessage.getMessageId()))
                .assertNext(found -> {
                    assert found.getMessageId().equals(testMessage.getMessageId());
                    assert found.getMessageType() == MessageType.PACS_008;
                    assert found.getStatus() == MessageStatus.RECEIVED;
                })
                .verifyComplete();
    }

    @Test
    void testFindByUetr() {
        messageRepository.save(testMessage).block();

        StepVerifier.create(messageRepository.findByUetr(testMessage.getUetr()))
                .assertNext(found -> {
                    assert found.getUetr().equals(testMessage.getUetr());
                })
                .verifyComplete();
    }

    @Test
    void testFindByStatus() {
        messageRepository.save(testMessage).block();

        StepVerifier.create(messageRepository.findByStatus(MessageStatus.RECEIVED))
                .assertNext(found -> {
                    assert found.getStatus() == MessageStatus.RECEIVED;
                })
                .verifyComplete();
    }

    @Test
    void testCountByStatus() {
        messageRepository.save(testMessage).block();

        StepVerifier.create(messageRepository.countByStatus(MessageStatus.RECEIVED))
                .assertNext(count -> {
                    assert count == 1L;
                })
                .verifyComplete();
    }

    @Test
    void testFindByEndToEndId() {
        messageRepository.save(testMessage).block();

        StepVerifier.create(messageRepository.findByEndToEndId(testMessage.getEndToEndId()))
                .assertNext(found -> {
                    assert found.getEndToEndId().equals(testMessage.getEndToEndId());
                })
                .verifyComplete();
    }
}
