# Ground Station Booking Service

## Overview

The Ground Station Booking Service is a comprehensive module integrated into YAMCS that enables satellite operators to schedule and manage ground station contacts. It provides a unified interface for booking satellite passes through multiple ground station network providers.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           YAMCS Web UI                                   │
│  ┌─────────────────────┐     ┌───────────────────────────────────────┐ │
│  │  Booking List View  │     │       Create Booking Wizard           │ │
│  │  - All bookings     │     │  1. Select Provider                   │ │
│  │  - Status display   │     │  2. Select Satellite                  │ │
│  │  - Refresh/Actions  │     │  3. Select Ground Station             │ │
│  └─────────────────────┘     │  4. Select Activity Scope             │ │
│                              │  5. Search Available Passes           │ │
│                              │  6. Reserve Contact                   │ │
│                              └───────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Angular Booking Service                           │
│                     (booking.service.ts)                                 │
│  - HTTP client for REST API calls                                        │
│  - TypeScript interfaces for data models                                │
│  - Provider API abstraction methods                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          REST API Layer                                  │
│                        (BookingApi.java)                                 │
│  Endpoints:                                                              │
│  - GET  /api/booking/providers                                          │
│  - GET  /api/booking/bookings                                           │
│  - POST /api/booking/bookings                                           │
│  - GET  /api/booking/bookings/pending                                   │
│  - POST /api/booking/bookings/{id}/approve                              │
│  - POST /api/booking/bookings/{id}/reject                               │
│  - GET  /api/booking/enums                                              │
│  - GET  /api/booking/provider/{provider}/satellites                     │
│  - GET  /api/booking/provider/{provider}/groundstations                 │
│  - GET  /api/booking/provider/{provider}/satellites/{id}/activityscopes │
│  - GET  /api/booking/provider/{provider}/contacts                       │
│  - POST /api/booking/provider/{provider}/reserve                        │
│  - POST /api/booking/provider/{provider}/cancel                         │
│  - GET  /api/booking/provider/{provider}/bookings                       │
└─────────────────────────────────────────────────────────────────────────┘
                          │                      │
                          ▼                      ▼
┌───────────────────────────────┐    ┌───────────────────────────────────┐
│      BookingDatabase          │    │      GsProviderFactory            │
│   (PostgreSQL via HikariCP)   │    │   (Provider Client Management)    │
│                               │    │                                   │
│  Tables:                      │    │  Supported Providers:             │
│  - gs_providers               │    │  - Dhruva Space (ISOCS API)       │
│  - gs_bookings                │    │  - Leafspace (planned)            │
│  - booking_approvals          │    │  - ISRO (planned)                 │
└───────────────────────────────┘    └───────────────────────────────────┘
                                                    │
                                                    ▼
                                     ┌───────────────────────────────────┐
                                     │     External Provider APIs        │
                                     │                                   │
                                     │  Dhruva Space ISOCS API:          │
                                     │  - List satellites                │
                                     │  - List ground stations           │
                                     │  - Get activity scopes            │
                                     │  - Query visibility windows       │
                                     │  - Reserve contacts               │
                                     │  - Cancel bookings                │
                                     └───────────────────────────────────┘
```

## Components

### Backend (Java)

#### BookingService.java
Main YAMCS service that initializes the database connection and manages the service lifecycle.

**Location:** `yamcs-core/src/main/java/org/yamcs/booking/BookingService.java`

```java
// Register as a global YAMCS service in yamcs.yaml:
services:
  - class: org.yamcs.booking.BookingService
```

#### BookingApi.java
REST API implementation that handles all HTTP endpoints for the booking system.

**Location:** `yamcs-core/src/main/java/org/yamcs/http/api/BookingApi.java`

Key responsibilities:
- CRUD operations for bookings
- Approval workflow (approve/reject)
- Provider API proxy (satellites, ground stations, contacts)
- Contact reservation with automatic database storage

#### BookingDatabase.java
Database access layer using HikariCP connection pooling for PostgreSQL.

**Location:** `yamcs-core/src/main/java/org/yamcs/booking/db/BookingDatabase.java`

Features:
- Connection pooling with HikariCP
- Environment variable configuration
- Graceful handling of database unavailability
- Transaction support for approval workflows

#### Provider Abstraction Layer

**Location:** `yamcs-core/src/main/java/org/yamcs/booking/provider/`

| File | Description |
|------|-------------|
| `GsProviderClient.java` | Interface for all provider implementations |
| `GsProviderFactory.java` | Singleton factory for provider client management |
| `IsocsProviderClient.java` | Dhruva Space ISOCS API implementation |
| `ProviderSatellite.java` | Satellite data model |
| `ProviderGroundStation.java` | Ground station data model |
| `ProviderActivityScope.java` | Activity scope configuration model |
| `ProviderContact.java` | Visibility window/pass data model |
| `ProviderBooking.java` | Booking result with timing metadata |

### Frontend (Angular)

#### booking.service.ts
Angular service that communicates with the backend REST API.

**Location:** `yamcs-web/src/main/webapp/projects/webapp/src/app/booking/booking.service.ts`

TypeScript interfaces:
- `GSProvider` - Ground station provider info
- `GSBooking` - Booking record
- `ProviderSatellite` - Satellite from provider API
- `ProviderGroundStation` - Ground station from provider API
- `ActivityScope` - Communication activity configuration
- `ProviderContact` - Available pass/visibility window
- `ProviderBookingInfo` - Reservation result
- `ReserveContactRequest` - Request payload for reservations

#### booking-list.component.ts
Displays all bookings in a table with status indicators and actions.

**Location:** `yamcs-web/src/main/webapp/projects/webapp/src/app/booking/booking-list/`

Features:
- Tabular display of all bookings
- Status badges (pending, approved, rejected)
- GS status indicators (scheduled, confirmed, completed)
- Refresh functionality
- Navigation to create new bookings

#### create-booking.component.ts
Multi-step wizard for creating new ground station bookings.

**Location:** `yamcs-web/src/main/webapp/projects/webapp/src/app/booking/create-booking/`

Workflow steps:
1. **Select Provider** - Choose ground station network (Dhruva, Leafspace, ISRO)
2. **Select Satellite** - Loaded dynamically from provider API
3. **Select Ground Station** - Available stations from provider
4. **Select Activity Scope** - Communication type (TM, TC, etc.)
5. **Search Passes** - Query available visibility windows by date range
6. **Reserve Contact** - Book the selected pass

### Protobuf Definitions

**Location:** `yamcs-api/src/main/proto/yamcs/protobuf/booking/booking_service.proto`

Defines:
- Service RPC methods with HTTP annotations
- Request/Response message types
- Data structures for all entities

### Database Schema

**Location:** `yamcs-core/src/main/resources/sql/booking_isocs_columns.sql`

#### Tables

**gs_providers**
```sql
CREATE TABLE gs_providers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type provider_type NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    api_endpoint VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**gs_bookings**
```sql
CREATE TABLE gs_bookings (
    id SERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    satellite_id VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP, -- computed from start_time + duration
    duration_minutes INTEGER NOT NULL,
    pass_type pass_type DEFAULT 'both',
    purpose purpose_type NOT NULL,
    rule_type booking_rule_type DEFAULT 'one_time',
    frequency_days INTEGER,
    notes TEXT,
    status booking_status DEFAULT 'pending',
    gs_status gs_status DEFAULT 'scheduled',
    requested_by VARCHAR(100) NOT NULL,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Provider-specific fields
    provider_satellite_id VARCHAR(100),
    provider_gs_id VARCHAR(100),
    provider_contact_id VARCHAR(100),
    provider_booking_id VARCHAR(100),
    provider_metadata JSONB,
    max_elevation DOUBLE PRECISION
);
```

**booking_approvals**
```sql
CREATE TABLE booking_approvals (
    id SERIAL PRIMARY KEY,
    booking_id INTEGER REFERENCES gs_bookings(id),
    approver VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL, -- 'approved' or 'rejected'
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Enum Types
```sql
CREATE TYPE provider_type AS ENUM ('leafspace', 'dhruva', 'isro');
CREATE TYPE pass_type AS ENUM ('uplink', 'downlink', 'both');
CREATE TYPE purpose_type AS ENUM ('telemetry', 'command', 'routine', 'emergency');
CREATE TYPE booking_rule_type AS ENUM ('one_time', 'recurring', 'on_demand');
CREATE TYPE booking_status AS ENUM ('pending', 'approved', 'rejected', 'cancelled');
CREATE TYPE gs_status AS ENUM ('scheduled', 'confirmed', 'in_progress', 'completed', 'failed');
```

## API Reference

### Booking Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/booking/providers` | List all active providers |
| GET | `/api/booking/bookings` | List all bookings |
| POST | `/api/booking/bookings` | Create a new booking (manual) |
| GET | `/api/booking/bookings/pending` | List pending approval bookings |
| POST | `/api/booking/bookings/{id}/approve` | Approve a booking |
| POST | `/api/booking/bookings/{id}/reject` | Reject a booking |
| GET | `/api/booking/enums` | Get enum values for forms |

### Provider API Proxy

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/booking/provider/{provider}/satellites` | List provider's satellites |
| GET | `/api/booking/provider/{provider}/groundstations` | List provider's ground stations |
| GET | `/api/booking/provider/{provider}/satellites/{id}/activityscopes` | Get activity scopes for satellite |
| GET | `/api/booking/provider/{provider}/contacts` | Search available passes |
| POST | `/api/booking/provider/{provider}/reserve` | Reserve a contact |
| POST | `/api/booking/provider/{provider}/cancel` | Cancel a booking |
| GET | `/api/booking/provider/{provider}/bookings` | List provider's bookings |

### Query Parameters for Contact Search

| Parameter | Type | Description |
|-----------|------|-------------|
| gsId | string | Ground station ID |
| satelliteId | string | Satellite ID |
| spbasId | string | Activity scope ID |
| startDate | string | Start date (YYYY-MM-DD) |
| endDate | string | End date (YYYY-MM-DD) |

## Configuration

### Environment Variables

```bash
# PostgreSQL Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=mcc
POSTGRES_USER=mcc_dbadmin
POSTGRES_PASSWORD=mcc_dbadmin

# Dhruva Space ISOCS API
ISOCS_BASE_URL=https://demoapi.astraview.in
ISOCS_EMAIL=your-email@example.com
ISOCS_PASSWORD=your-password
```

### YAMCS Configuration (yamcs.yaml)

```yaml
services:
  - class: org.yamcs.booking.BookingService
```

## Workflow

### Creating a Booking via Provider API

1. **User selects provider** (e.g., "Dhruva Space")
2. **System fetches satellites** from provider API
3. **User selects satellite** and system loads ground stations
4. **User selects ground station** and activity scope
5. **User specifies date range** and searches for passes
6. **System displays available contacts** with timing and elevation
7. **User selects a pass** and clicks "Reserve Contact"
8. **System calls provider API** to make reservation
9. **Booking is stored** in local database with provider metadata
10. **Confirmation shown** to user with booking details

### Approval Workflow (Manual Bookings)

1. Booking created with status `pending`
2. Admin views pending bookings list
3. Admin approves or rejects with comments
4. Approval logged in `booking_approvals` table
5. Status updated to `approved` or `rejected`

## Data Flow for Telecommand Triggering

When a booking is reserved through a provider, the system stores comprehensive metadata to enable automatic telecommand triggering:

```json
{
  "noradId": 12345,
  "satelliteName": "SATELLITE-1",
  "groundStationName": "Station Alpha",
  "gsapName": "Dhruva",
  "activityScope": "TM",
  "gsabracId": "abc123",
  "spbasId": "def456",
  "gsaId": "ghi789",
  "gsapId": "jkl012",
  "bookingStartEpochMs": 1704067200000,
  "bookingEndEpochMs": 1704068100000,
  "recordingStartTime": "2024-01-01T00:00:00Z",
  "recordingEndTime": "2024-01-01T00:15:00Z"
}
```

This metadata enables:
- Scheduled command uploads before pass start
- Automatic recording triggers
- Pass timeline visualization
- Post-pass data processing

## Provider Implementation Guide

To add a new ground station provider:

1. **Create client class** implementing `GsProviderClient`:
```java
public class NewProviderClient implements GsProviderClient {
    @Override
    public String getProviderType() { return "newprovider"; }

    @Override
    public List<ProviderSatellite> listSatellites() { ... }

    @Override
    public List<ProviderGroundStation> listGroundStations() { ... }

    // Implement all interface methods
}
```

2. **Register in GsProviderFactory**:
```java
case "newprovider":
    return new NewProviderClient(baseUrl, credentials);
```

3. **Update isSupported() method**:
```java
return key.equals("dhruva") || key.equals("newprovider");
```

4. **Add provider to database**:
```sql
INSERT INTO gs_providers (name, type, api_endpoint, is_active)
VALUES ('New Provider', 'newprovider', 'https://api.newprovider.com', true);
```

## File Structure

```
yamcs/
├── yamcs-api/
│   └── src/main/proto/yamcs/protobuf/booking/
│       └── booking_service.proto          # Protobuf definitions
├── yamcs-core/
│   └── src/main/java/org/yamcs/
│       ├── booking/
│       │   ├── BookingService.java        # Main YAMCS service
│       │   ├── db/
│       │   │   └── BookingDatabase.java   # Database access layer
│       │   ├── model/
│       │   │   ├── GSBooking.java         # Booking model
│       │   │   └── GSProvider.java        # Provider model
│       │   └── provider/
│       │       ├── GsProviderClient.java  # Provider interface
│       │       ├── GsProviderFactory.java # Provider factory
│       │       ├── IsocsProviderClient.java # Dhruva implementation
│       │       ├── ProviderSatellite.java
│       │       ├── ProviderGroundStation.java
│       │       ├── ProviderActivityScope.java
│       │       ├── ProviderContact.java
│       │       └── ProviderBooking.java
│       └── http/api/
│           └── BookingApi.java            # REST API
├── yamcs-web/
│   └── src/main/webapp/projects/webapp/src/app/booking/
│       ├── booking.service.ts             # Angular service
│       ├── booking-list/
│       │   ├── booking-list.component.ts
│       │   ├── booking-list.component.html
│       │   └── booking-list.component.css
│       └── create-booking/
│           ├── create-booking.component.ts
│           ├── create-booking.component.html
│           └── create-booking.component.css
└── docs/
    └── BOOKING_SERVICE.md                 # This documentation
```

## Status Definitions

### Booking Status (`status`)
| Status | Description |
|--------|-------------|
| pending | Awaiting approval |
| approved | Approved for execution |
| rejected | Rejected by approver |
| cancelled | Cancelled by user |

### Ground Station Status (`gs_status`)
| Status | Description |
|--------|-------------|
| scheduled | Booking created, not yet confirmed with provider |
| confirmed | Confirmed with ground station provider |
| in_progress | Pass currently active |
| completed | Pass completed successfully |
| failed | Pass failed or was missed |

## Future Enhancements

- [ ] Leafspace provider integration
- [ ] ISRO provider integration
- [ ] Recurring booking rules (daily, weekly patterns)
- [ ] Conflict detection and resolution
- [ ] Calendar view for bookings
- [ ] Email/notification alerts
- [ ] Bulk booking operations
- [ ] Historical pass analytics
- [ ] Cost estimation and tracking
