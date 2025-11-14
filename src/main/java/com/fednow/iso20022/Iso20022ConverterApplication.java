package com.fednow.iso20022;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Main application class for ISO 20022 Message Converter for FedNow.
 *
 * This application provides reactive message conversion services for ISO 20022
 * payment messages used in the FedNow instant payment system.
 *
 * Key Features:
 * - Reactive REST API with WebFlux
 * - PostgreSQL with R2DBC for reactive database access
 * - Woodstox XML processing
 * - OpenAPI/Swagger documentation
 * - 25+ message converters for ISO 20022 standards
 *
 * @author FedNow Development Team
 * @version 1.0.0
 * @since 2024-01-15
 */
@SpringBootApplication
@EnableR2dbcRepositories
@OpenAPIDefinition(
    info = @Info(
        title = "ISO 20022 Message Converter API",
        version = "1.0.0",
        description = """
            Reactive REST API for converting between ISO 20022 message formats used in FedNow.

            ## Supported Conversions

            ### Phase 1 - Critical Converters
            - **pain.001 → pacs.008**: Customer payment to interbank transfer
            - **pacs.002 → pain.002**: Payment status to customer notification
            - **pacs.004 → pain.007**: Payment return to customer reversal
            - **pacs.008 → camt.054**: Interbank transfer to account notification
            - **pacs.008 → pacs.002**: Generate payment status report
            - **pacs.008 → admi.002**: Generate system event notification

            ### Message Flow
            1. Customer initiates payment (pain.001)
            2. Bank converts to interbank format (pacs.008)
            3. FedNow processes and returns status (pacs.002)
            4. Bank converts status to customer format (pain.002)
            5. Account notifications generated (camt.054)

            ## Technical Details
            - Java 21 with virtual threads
            - Spring Boot 3.x with WebFlux
            - R2DBC for reactive PostgreSQL access
            - Woodstox XML processing
            - Comprehensive validation framework
            """,
        contact = @Contact(
            name = "FedNow Support",
            email = "support@fednow.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    ),
    servers = {
        @Server(
            url = "http://localhost:8080",
            description = "Local Development Server"
        ),
        @Server(
            url = "https://api.fednow.com",
            description = "Production Server"
        )
    }
)
public class Iso20022ConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(Iso20022ConverterApplication.class, args);
    }
}
