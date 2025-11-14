package com.fednow.iso20022.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration for FedNow ISO 20022 Message Converter API.
 *
 * Provides comprehensive API documentation for all converter endpoints.
 *
 * API Features:
 * - Reactive endpoints using Spring WebFlux
 * - ISO 20022 message conversion for FedNow
 * - Real-time payment processing
 * - Account reporting and statements
 * - Direct debit and mandate management
 *
 * Access Swagger UI at: /swagger-ui.html
 * Access OpenAPI JSON at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fednowConverterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FedNow ISO 20022 Message Converter API")
                        .description("""
                                RESTful API for converting ISO 20022 messages for FedNow instant payment processing.

                                ## Overview

                                This API provides endpoints for converting between various ISO 20022 message formats
                                used in the FedNow instant payment system. It supports:

                                - **Customer to Interbank Conversions**: pain.001 → pacs.008, pain.008 → pacs.003
                                - **Status and Response Generation**: pacs.008 → pacs.002, pacs.002 → pain.002
                                - **Returns and Reversals**: pacs.008 → pacs.004, pacs.008 → pacs.007, camt.056 → pacs.004
                                - **Account Reporting**: pacs.008 → camt.052, multiple pacs.008 → camt.052/camt.053
                                - **System Events**: pacs.008 → admi.002, any message → admi.007
                                - **Mandate Management**: pain.009 → pacs.008, pain.013 → pacs.028

                                ## Message Types

                                ### Customer Payments (pain.*)
                                - **pain.001**: Customer Credit Transfer Initiation
                                - **pain.002**: Customer Payment Status Report
                                - **pain.007**: Customer Payment Reversal
                                - **pain.008**: Customer Direct Debit Initiation
                                - **pain.009**: Mandate Initiation Request
                                - **pain.013**: Creditor Payment Activation Request

                                ### Interbank Payments (pacs.*)
                                - **pacs.002**: Payment Status Report (FI to FI)
                                - **pacs.003**: FI to FI Customer Direct Debit
                                - **pacs.004**: Payment Return
                                - **pacs.007**: FI to FI Payment Reversal
                                - **pacs.008**: FI to FI Customer Credit Transfer
                                - **pacs.028**: FI to FI Payment Status Request

                                ### Cash Management (camt.*)
                                - **camt.052**: Bank to Customer Account Report
                                - **camt.053**: Bank to Customer Account Statement
                                - **camt.054**: Bank to Customer Debit/Credit Notification
                                - **camt.056**: FI to FI Payment Cancellation Request

                                ### Administration (admi.*)
                                - **admi.002**: System Event Notification
                                - **admi.007**: Receipt Acknowledgement

                                ## FedNow Specifics

                                All conversions comply with FedNow requirements:
                                - Settlement Method: INDA (Instructed Agent)
                                - Clearing System: FDW (FedNow)
                                - Settlement: T+0 (same day)
                                - UETR tracking for all payments
                                - Maximum amount: $500,000 per transaction
                                - 24/7/365 availability

                                ## Authentication

                                All endpoints require proper authentication and authorization.
                                Contact your system administrator for API credentials.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FedNow Payment Operations")
                                .email("payments@example.com")
                                .url("https://fednow.example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://fednow.example.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api-staging.fednow.example.com")
                                .description("Staging Server"),
                        new Server()
                                .url("https://api.fednow.example.com")
                                .description("Production Server")
                ));
    }
}
