# Infrastructure Layer Implementation

## Summary

Added complete database schema and JPA/R2DBC entity classes for the FedNow ISO 20022 converter infrastructure.

## Database Schema Enhancements

### New Enum Types (6)
1. `account_status` - ACTIVE, BLOCKED, CLOSED, SUSPENDED, DORMANT
2. `account_type` - CHECKING, SAVINGS, LOAN, INVESTMENT, CREDIT_CARD, MONEY_MARKET
3. `mandate_status` - ACTIVE, SUSPENDED, REVOKED, EXPIRED, PENDING
4. `reversal_status` - REQUESTED, ACCEPTED, SETTLED, REJECTED, EXPIRED
5. `investigation_status` - OPEN, PENDING, RESOLVED, CLOSED, ESCALATED
6. `rfp_status` - REQUESTED, ACCEPTED, REJECTED, CANCELLED, EXPIRED

### New Database Tables (7)

**1. accounts** - Customer and institutional accounts
- Account number (IBAN format)
- Account type and status
- Balance tracking with overdraft support
- OFAC status tracking
- Fraud score (0-100)
- Full audit trail

**2. bic_directory** - Bank Identification Codes
- BIC/SWIFT codes (8 or 11 characters)
- Institution details and addresses
- FedNow participant flag
- ABA routing numbers (US)
- Active/inactive status tracking

**3. mandates** - Direct Debit Mandates
- Mandate types: RCUR, OOFF, FNAL, FRST
- Creditor and debtor information
- Max amount and frequency
- Activation/expiration dates
- Usage tracking

**4. reversals** - Payment Reversals
- Original payment tracking
- 15-second FedNow window enforcement
- Reversal deadline tracking
- Status: REQUESTED → ACCEPTED → SETTLED
- Within-window validation

**5. account_transactions** - Transaction History
- Linked to accounts and messages
- Transaction types: DEBIT, CREDIT, HOLD, RELEASE, FEE, REVERSAL
- Balance before/after tracking
- Counterparty information
- Booking and value dates

**6. rfp_requests** - Request for Payment
- Pain.013/Pain.014 message tracking
- Invoice information
- Due dates and expiration
- Request/response message links
- Related payment tracking

**7. investigations** - Payment Investigations
- Camt.028/Camt.029 tracking
- Investigation types: MISSING_PAYMENT, DUPLICATE_PAYMENT, WRONG_AMOUNT, etc.
- Priority levels: LOW, MEDIUM, HIGH, URGENT
- SLA deadline tracking
- Resolution tracking

### Schema Statistics
- **Total Tables**: 14 (7 existing + 7 new)
- **Total Enum Types**: 9 (3 existing + 6 new)
- **Total Indexes**: 80+ for optimal query performance
- **Triggers**: 6 updated_at triggers + 1 audit trigger
- **Views**: 3 (active_payments, payment_status_summary, converter_performance)

## Java Entity Classes

### Enums Created (8)
Located in `com.fednow.iso20022.entity.enums`:
1. `MessageType` - 28 ISO 20022 message types
2. `MessageStatus` - 10 message lifecycle statuses
3. `PaymentStatusCode` - 10 ISO 20022 status codes
4. `AccountStatus` - 5 account states
5. `AccountType` - 6 account categories
6. `MandateStatus` - 5 mandate states
7. `ReversalStatus` - 5 reversal states
8. `InvestigationStatus` - 5 investigation states
9. `RfpStatus` - 5 RFP states

### R2DBC Entities Created (4)
Located in `com.fednow.iso20022.entity`:

**1. Message.java**
- Maps to `messages` table
- 24 fields including UETR, E2E ID, transaction tracking
- Full ISO 20022 message lifecycle support
- JSONB metadata support

**2. Account.java**
- Maps to `accounts` table
- 21 fields including balance, OFAC status, fraud score
- Overdraft limit support
- Account holder tracking

**3. BicDirectory.java**
- Maps to `bic_directory` table
- 18 fields including BIC, routing number
- FedNow participant tracking
- Address information

**4. Mandate.java**
- Maps to `mandates` table
- 18 fields including mandate type, frequency
- Max amount enforcement
- Usage tracking

## Key Features

### Database Design
- **UUID primary keys** for distributed systems
- **Comprehensive indexing** for high-performance queries
- **JSONB metadata** columns for flexibility
- **Foreign key constraints** with cascade rules
- **Check constraints** for data integrity
- **Automatic timestamps** via triggers

### R2DBC Integration
- Reactive, non-blocking database access
- PostgreSQL-specific features (JSONB, UUID)
- Enum mapping to PostgreSQL enum types
- Instant and LocalDate temporal support

### FedNow Compliance
- **15-second reversal window** tracking in `reversals` table
- **OFAC screening** integration fields
- **Fraud detection** score tracking
- **Settlement tracking** with T+0 support
- **UETR** (UUID v4) for transaction tracing

## Next Steps

### Repositories (Pending)
- MessageRepository
- AccountRepository
- BicDirectoryRepository
- MandateRepository
- ReversalRepository
- AccountTransactionRepository
- RfpRequestRepository
- InvestigationRepository

### Services (Pending)
- MessageProcessingService
- ValidationService
- OFACScreeningService
- FraudDetectionService
- AccountService

### External Clients (Pending)
- OFACClient
- FraudDetectionClient
- FedNowClient

## Files Modified
- `src/main/resources/schema.sql` (+400 lines)

## Files Created
- 8 enum classes
- 4 entity classes

## Total Lines of Code
- SQL: ~400 lines
- Java: ~800 lines
- **Total**: ~1,200 lines
