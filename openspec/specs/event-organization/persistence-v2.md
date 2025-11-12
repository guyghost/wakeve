# Specification: Event Organization - Data Persistence (Phase 2)

> **Capability**: `event-organization`
> **Version**: 2.0 (Persistence Layer)
> **Status**: Draft
> **Phase**: Phase 2

## Purpose

Add persistent storage for event organization data using SQLDelight, enabling offline-first capability, data survival across app restarts, and foundation for multi-device synchronization.

## Core Concepts

**SQLite Database**: Local persistent storage using SQLite, accessed through SQLDelight for type-safe queries.

**Schema**: Database tables for events, time slots, polls, and votes with proper relationships and constraints.

**Persistence Layer**: Database-backed implementation of EventRepository that replaces in-memory storage.

**Migrations**: Version-controlled schema changes to support future database evolution.

**Offline-First**: Local database as source of truth, with sync to backend when online.

## Requirements

### The system SHALL store all event data in a local SQLite database

#### Scenario: Event persists after app restart
**Given**: User creates and saves an event
**When**: User closes and reopens the app
**Then**: Event data is intact with all original values

### The system SHALL use SQLDelight for type-safe database queries

#### Scenario: Queries are type-checked at compile time
**Given**: Database schema is defined in .sq files
**When**: Kotlin code references database entities
**Then**: Invalid queries fail at compile time, not runtime

### The system SHALL store all timestamps as ISO 8601 UTC strings

#### Scenario: Timestamps are timezone-neutral
**Given**: Event deadline is 2025-12-25T18:00:00Z
**When**: Stored in database and retrieved
**Then**: Value remains exactly 2025-12-25T18:00:00Z (no conversion)

### The system SHALL support database schema migrations

#### Scenario: Schema change from v1 to v2
**Given**: Database exists at v1 schema
**When**: New version adds a column
**Then**: Migration runs automatically and data is preserved

### The system SHALL maintain referential integrity for events and polls

#### Scenario: Delete event cascades to polls
**Given**: Event with associated poll exists
**When**: Event is deleted
**Then**: Associated poll is also deleted (cascading)

### The system SHALL implement optimistic locking for concurrent writes

#### Scenario: Update fails if data changed since read
**Given**: User reads event at version 1
**When**: Another user updates event to version 2, then first user tries to update
**Then**: Update fails with conflict error

### The system SHALL provide database query performance monitoring

#### Scenario: Slow queries are logged
**Given**: Database query takes >100ms
**When**: Query executes
**Then**: Query time and parameters are logged for analysis

### The system SHALL support database encryption at rest

#### Scenario: Database file is encrypted
**Given**: Sensitive user data (tokens, personal info) is stored
**When**: Database file is examined on disk
**Then**: Content is encrypted and unreadable without decryption key

### The system SHALL backup database state for recovery

#### Scenario: Database backup exists for recovery
**Given**: User experiences data corruption
**When**: Recovery is requested
**Then**: Previous backup can be restored

### The system SHALL provide database statistics and health checks

#### Scenario: Database health can be verified
**Given**: Application starts
**When**: Health check runs
**Then**: Database integrity is verified and stats are logged
