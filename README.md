# ISO 20022 Message Converter for FedNow

Reactive message converter system for ISO 20022 payment messages used in the FedNow instant payment system.

## Technology Stack

- **Java 21** - Latest LTS with virtual threads
- **Spring Boot 3.2.0** - Framework
- **Spring WebFlux** - Reactive REST APIs
- **Spring Data R2DBC** - Reactive database access
- **PostgreSQL** - Message persistence
- **Woodstox 6.6.0** - XML processing
- **OpenAPI/Swagger 2.3.0** - API documentation

## Features

### Phase 1 - Critical Converters

1. **CustomerCreditTransferToPacs008Converter** ⭐
   - `pain.001 → pacs.008`: Customer payment to FedNow interbank transfer
   - Most important converter for payment initiation

2. **Pacs002ToPain002Converter** ⭐
   - `pacs.002 → pain.002`: Status report to customer notification
   - Translates technical codes to customer-friendly messages

3. **Pacs004ToPain007Converter** ⭐
   - `pacs.004 → pain.007`: Return notification to customer
   - Handles payment returns and reversals

4. **Pacs008ToCamt054Converter** ⭐
   - `pacs.008 → camt.054`: Real-time account notifications
   - Credit/debit notifications for customers

5. **Pacs008ToPacs002Converter** ⭐
   - `pacs.008 → pacs.002`: Generate payment status reports
   - Acceptance/rejection responses

6. **Pacs008ToAdmi002Converter**
   - `pacs.008 → admi.002`: System event notifications
   - Technical/operational issue notifications

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- PostgreSQL 15+
- Docker (optional)

### Run with Docker Compose

```bash
docker-compose up
```

### Run Locally

1. Start PostgreSQL:
```bash
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=fednow \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15
```

2. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

3. Access Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Conversion Endpoints

- `POST /api/v1/convert/pain001-to-pacs008` - Customer payment to interbank
- `POST /api/v1/convert/pacs002-to-pain002` - Status to customer notification
- `POST /api/v1/convert/pacs004-to-pain007` - Return to customer reversal
- `POST /api/v1/convert/pacs008-to-camt054` - Interbank to account notification
- `POST /api/v1/convert/pacs008-to-pacs002` - Generate payment status
- `POST /api/v1/convert/pacs008-to-admi002` - Generate system event

### Validation Endpoints

- `POST /api/v1/validate/pain001` - Validate customer payment
- `POST /api/v1/validate/pacs008` - Validate interbank transfer

### Message Tracking

- `GET /api/v1/messages/{id}` - Get message by ID
- `GET /api/v1/messages/uetr/{uetr}` - Track by UETR
- `GET /api/v1/messages/end-to-end/{e2eId}` - Track by End-to-End ID

## Architecture

```
┌─────────────────────────────────────────────┐
│  Customer Payment (pain.001)                │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  CustomerCreditTransferToPacs008Converter   │
│  - Validate customer payment                │
│  - Generate UETR                            │
│  - Add bank identifiers                     │
│  - Add settlement information               │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  Interbank Transfer (pacs.008)              │
│  → Sent to FedNow                           │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  FedNow Processing                          │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  Payment Status (pacs.002)                  │
│  ← Received from FedNow                     │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  Pacs002ToPain002Converter                  │
│  - Translate status codes                   │
│  - Generate customer-friendly messages      │
└─────────────────┬───────────────────────────┘
                  ↓
┌─────────────────────────────────────────────┐
│  Customer Status Report (pain.002)          │
└─────────────────────────────────────────────┘
```

## Project Structure

```
src/
├── main/
│   ├── java/com/fednow/iso20022/
│   │   ├── config/              # Configuration classes
│   │   ├── controller/          # REST API controllers
│   │   ├── converter/           # Message converters
│   │   │   ├── core/            # Converter interfaces
│   │   │   ├── phase1/          # Phase 1 critical converters
│   │   │   └── utils/           # Converter utilities
│   │   ├── domain/              # Domain models
│   │   │   ├── pain/            # pain.* messages
│   │   │   ├── pacs/            # pacs.* messages
│   │   │   ├── camt/            # camt.* messages
│   │   │   └── admi/            # admi.* messages
│   │   ├── exception/           # Exception handling
│   │   ├── repository/          # R2DBC repositories
│   │   ├── service/             # Business services
│   │   │   ├── converter/       # Converter services
│   │   │   ├── validation/      # Validation services
│   │   │   └── xml/             # XML processing services
│   │   └── validation/          # Validators
│   └── resources/
│       ├── application.yml      # Main configuration
│       ├── schema.sql           # Database schema
│       └── schemas/             # ISO 20022 XSD schemas
└── test/
    └── java/com/fednow/iso20022/
        ├── converter/           # Converter tests
        ├── integration/         # Integration tests
        └── service/             # Service tests
```

## Configuration

### Database Configuration

Environment variables:
- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name (default: fednow)
- `DB_USER` - Database user (default: postgres)
- `DB_PASSWORD` - Database password (default: postgres)

### FedNow Configuration

```yaml
iso20022:
  fednow:
    settlement-method: INDA
    clearing-system: FDW
    amount-limit-min: 0.01
    amount-limit-max: 500000.00
    currency: USD
```

## Development

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Code Coverage

```bash
mvn jacoco:report
```

## License

Apache License 2.0

## Support

For support and questions, contact: support@fednow.com
