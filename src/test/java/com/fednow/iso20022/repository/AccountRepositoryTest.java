package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Account;
import com.fednow.iso20022.entity.enums.AccountStatus;
import com.fednow.iso20022.entity.enums.AccountType;
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
import java.time.LocalDate;

/**
 * Integration tests for AccountRepository using Testcontainers
 */
@DataR2dbcTest
@Testcontainers
class AccountRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("fednow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        accountRepository.deleteAll().block();

        // Create test account
        testAccount = new Account();
        testAccount.setAccountNumber("US12345678901234567890");
        testAccount.setAccountType(AccountType.CHECKING);
        testAccount.setAccountStatus(AccountStatus.ACTIVE);
        testAccount.setAccountHolderName("John Doe");
        testAccount.setAccountHolderId("TAX123456");
        testAccount.setInstitutionBic("BANKUS33XXX");
        testAccount.setCurrency("USD");
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setAvailableBalance(new BigDecimal("9500.00"));
        testAccount.setOverdraftLimit(new BigDecimal("500.00"));
        testAccount.setOpenedDate(LocalDate.now());
        testAccount.setOfacStatus("CLEAR");
        testAccount.setFraudScore(0);
        testAccount.setCreatedAt(Instant.now());
        testAccount.setUpdatedAt(Instant.now());
    }

    @Test
    void testSaveAndFindById() {
        StepVerifier.create(accountRepository.save(testAccount))
                .assertNext(saved -> {
                    assert saved.getId() != null;
                    assert saved.getAccountNumber().equals(testAccount.getAccountNumber());
                })
                .verifyComplete();
    }

    @Test
    void testFindByAccountNumber() {
        accountRepository.save(testAccount).block();

        StepVerifier.create(accountRepository.findByAccountNumber(testAccount.getAccountNumber()))
                .assertNext(found -> {
                    assert found.getAccountNumber().equals(testAccount.getAccountNumber());
                    assert found.getAccountType() == AccountType.CHECKING;
                    assert found.getAccountStatus() == AccountStatus.ACTIVE;
                    assert found.getBalance().compareTo(new BigDecimal("10000.00")) == 0;
                })
                .verifyComplete();
    }

    @Test
    void testFindByInstitutionBic() {
        accountRepository.save(testAccount).block();

        StepVerifier.create(accountRepository.findByInstitutionBic(testAccount.getInstitutionBic()))
                .assertNext(found -> {
                    assert found.getInstitutionBic().equals(testAccount.getInstitutionBic());
                })
                .verifyComplete();
    }

    @Test
    void testFindByAccountStatus() {
        accountRepository.save(testAccount).block();

        StepVerifier.create(accountRepository.findByAccountStatus(AccountStatus.ACTIVE))
                .assertNext(found -> {
                    assert found.getAccountStatus() == AccountStatus.ACTIVE;
                })
                .verifyComplete();
    }

    @Test
    void testFindByAccountHolderName() {
        accountRepository.save(testAccount).block();

        StepVerifier.create(accountRepository.findByAccountHolderNameContainingIgnoreCase("john"))
                .assertNext(found -> {
                    assert found.getAccountHolderName().toLowerCase().contains("john");
                })
                .verifyComplete();
    }

    @Test
    void testAccountsWithHighFraudScore() {
        // Create account with high fraud score
        testAccount.setFraudScore(85);
        accountRepository.save(testAccount).block();

        StepVerifier.create(accountRepository.findAccountsWithHighFraudScore(80))
                .assertNext(found -> {
                    assert found.getFraudScore() >= 80;
                })
                .verifyComplete();
    }
}
