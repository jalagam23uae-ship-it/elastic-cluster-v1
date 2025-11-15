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
- Docker & Docker Compose (recommended)

### Run with Docker Compose (Recommended)

The easiest way to run the application with all dependencies:

```bash
# Start all services (app, database, monitoring)
docker-compose up -d

# View logs
docker-compose logs -f iso20022-converter

# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v
```

**Access Points:**
- **Application API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics
- **Prometheus:** http://localhost:9090
- **Grafana:** http://localhost:3000 (admin/admin)

### Run Locally (Without Docker)

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

**Total Endpoints: 25** (Phase 1: 12, Phase 2: 4, Phase 3: 4, Phase 4: 3, Common: 2)

### Phase 1: Customer Payments & Critical Operations (12 Endpoints)

#### Payment Initiation
- `POST /api/v1/convert/payments/pain001-to-pacs008` - Customer payment to interbank

#### Status & Notifications
- `POST /api/v1/convert/payments/pacs008-to-pacs002` - Generate payment status
- `POST /api/v1/convert/payments/pacs002-to-pain002` - Status to customer notification
- `POST /api/v1/convert/payments/pacs008-to-camt054` - Interbank to account notification

#### Returns & Reversals
- `POST /api/v1/convert/payments/pacs004-to-pain007` - Return to customer reversal
- `POST /api/v1/convert/payments/pacs008-to-pacs004` - Payment to return (NEW)
- `POST /api/v1/convert/payments/pacs004-to-pacs002` - Return acknowledgment (NEW)
- `POST /api/v1/convert/payments/pacs007-to-pacs002` - Reversal acknowledgment (NEW)

#### Error & Event Handling
- `POST /api/v1/convert/payments/pacs008-to-admi002` - Generate system event
- `POST /api/v1/convert/errors/any-to-admi002` - Generic system errors (NEW)
- `POST /api/v1/convert/security/auth-failure-to-admi002` - Security events (NEW)
- `POST /api/v1/convert/validation/schema-failure-to-admi002` - Validation errors (NEW)

### Validation Endpoints

- `POST /api/v1/validate/pain001` - Validate customer payment
- `POST /api/v1/validate/pacs008` - Validate interbank transfer

### Message Tracking

- `GET /api/v1/messages/{id}` - Get message by ID
- `GET /api/v1/messages/uetr/{uetr}` - Track by UETR
- `GET /api/v1/messages/end-to-end/{e2eId}` - Track by End-to-End ID

**For complete API documentation, see:**
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- REST_API_ENDPOINTS_ADDED.md for detailed endpoint specifications

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

## Docker Deployment

### Architecture

The Docker Compose setup includes the following services:

1. **iso20022-converter** - Spring Boot application (port 8080)
2. **postgres** - PostgreSQL 15 database (port 5432)
3. **prometheus** - Metrics collection (port 9090)
4. **grafana** - Metrics visualization (port 3000)

### Building and Running

#### Start All Services

```bash
# Build and start in detached mode
docker-compose up -d --build

# Check service status
docker-compose ps

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f iso20022-converter
```

#### Stop Services

```bash
# Stop services (preserve data)
docker-compose stop

# Stop and remove containers (preserve volumes)
docker-compose down

# Stop, remove containers, and delete all data
docker-compose down -v
```

#### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart iso20022-converter
```

### Environment Variables

You can customize the deployment by setting environment variables:

```bash
# Create .env file
cat > .env << EOF
# Database Configuration
POSTGRES_DB=fednow
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-secure-password

# Application Configuration
SPRING_PROFILES_ACTIVE=docker
LOGGING_LEVEL_COM_FEDNOW=DEBUG

# FedNow Configuration
FEDNOW_VALIDATION_ENABLED=true
FEDNOW_REVERSAL_WINDOW_SECONDS=15
FEDNOW_MAX_TRANSACTION_AMOUNT=500000
EOF

# Start with custom environment
docker-compose up -d
```

### Health Checks

All services include health checks:

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check PostgreSQL health
docker-compose exec postgres pg_isready -U postgres

# View detailed health information
curl http://localhost:8080/actuator/health | jq
```

### Monitoring

#### Prometheus

Access Prometheus at http://localhost:9090

**Useful Queries:**
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
```

#### Grafana

Access Grafana at http://localhost:3000
- **Username:** admin
- **Password:** admin

Prometheus datasource is automatically configured.

**Creating Dashboards:**
1. Navigate to Dashboards → New → Import
2. Use dashboard ID `4701` for JVM metrics
3. Use dashboard ID `6756` for Spring Boot metrics

### Production Deployment

#### Resource Limits

The docker-compose.yml includes resource limits:

- **Application:** 2GB max, 1GB reserved
- **PostgreSQL:** 512MB max, 256MB reserved
- **Prometheus:** 512MB max, 256MB reserved
- **Grafana:** 256MB max, 128MB reserved

Adjust in `docker-compose.yml` as needed for your environment.

#### Security Considerations

1. **Change default passwords:**
   ```yaml
   # docker-compose.yml
   postgres:
     environment:
       POSTGRES_PASSWORD: your-secure-password

   grafana:
     environment:
       GF_SECURITY_ADMIN_PASSWORD: your-secure-password
   ```

2. **Use secrets management:**
   ```bash
   # Use Docker secrets for production
   echo "your-db-password" | docker secret create db_password -
   ```

3. **Enable TLS/SSL:**
   - Configure Spring Boot with SSL certificates
   - Use reverse proxy (nginx/traefik) for TLS termination

#### Backup and Restore

**Backup PostgreSQL:**
```bash
# Create backup
docker-compose exec postgres pg_dump -U postgres fednow > backup.sql

# Restore backup
docker-compose exec -T postgres psql -U postgres fednow < backup.sql
```

**Backup Volumes:**
```bash
# Backup all volumes
docker run --rm -v elastic-cluster-v1_postgres-data:/data \
  -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz /data
```

### Troubleshooting

#### Application Won't Start

```bash
# Check logs
docker-compose logs iso20022-converter

# Check if PostgreSQL is ready
docker-compose exec postgres pg_isready -U postgres

# Rebuild without cache
docker-compose build --no-cache iso20022-converter
docker-compose up -d
```

#### Database Connection Issues

```bash
# Verify database is accessible
docker-compose exec iso20022-converter nc -zv postgres 5432

# Check database logs
docker-compose logs postgres

# Reset database
docker-compose down -v
docker-compose up -d
```

#### Out of Memory

```bash
# Check container resource usage
docker stats

# Increase memory limits in docker-compose.yml
# or adjust JVM options:
environment:
  JAVA_OPTS: "-XX:MaxRAMPercentage=50.0"
```

#### Network Issues

```bash
# Inspect network
docker network inspect elastic-cluster-v1_fednow-network

# Restart network
docker-compose down
docker-compose up -d
```

### Performance Tuning

#### JVM Options

Customize JVM settings in `docker-compose.yml`:

```yaml
iso20022-converter:
  environment:
    JAVA_OPTS: >-
      -XX:+UseContainerSupport
      -XX:MaxRAMPercentage=75.0
      -XX:InitialRAMPercentage=50.0
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:+AlwaysPreTouch
      -XX:+UseStringDeduplication
```

#### PostgreSQL Tuning

Add to `docker-compose.yml`:

```yaml
postgres:
  command:
    - postgres
    - -c
    - max_connections=200
    - -c
    - shared_buffers=256MB
    - -c
    - effective_cache_size=1GB
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
