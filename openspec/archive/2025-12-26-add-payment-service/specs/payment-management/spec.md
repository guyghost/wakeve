# Payment Management Specification

## Overview
The Payment Service manages cagnottes (pots) for Wakeve events and integrates with Tricount for cost sharing and expense tracking.

## ADDED Requirements

### Requirement: Payment Service Creation
The system MUST provide a PaymentService to manage event cagnottes and Tricount integration.

#### Scenario: Create Pot
**WHEN** organizer creates a cagnotte for an event with goal amount
**THEN** pot is created with Tricount group setup
**AND** pot ID is returned for future operations

#### Scenario: Add Contribution
**WHEN** participant adds a contribution to active pot
**THEN** contribution is saved and pot balance updated
**AND** Tricount expense is created if applicable

#### Scenario: Get Pot Balance
**WHEN** requesting pot balance for an event
**THEN** returns current balance, percentage, and contributor count

#### Scenario: Close Pot
**WHEN** organizer closes a completed pot
**THEN** status changes to closed and notifications sent

#### Scenario: Generate Tricount Link
**WHEN** requesting Tricount link for pot management
**THEN** returns valid Tricount URL with balance information

#### Scenario: Export Tricount Data
**WHEN** exporting Tricount data for reporting
**THEN** returns expenses, members, and balances data

#### Scenario: Update Pot Details
**WHEN** updating pot title, goal amount, or currency
**THEN** pot is updated and Tricount group synchronized

#### Scenario: Get Payment Statistics
**WHEN** requesting payment statistics for event
**THEN** returns totals, averages, and participant balances

### Requirement: Payment Data Models
The system MUST define data models for pots, contributions, and payment statistics.

#### Scenario: Pot Model
**GIVEN** pot creation parameters
**WHEN** pot is instantiated
**THEN** all required fields are set with proper defaults

#### Scenario: Contribution Model
**GIVEN** contribution parameters
**WHEN** contribution is created
**THEN** amount, description, and sharing details are stored

#### Scenario: Balance Calculations
**GIVEN** list of contributions
**WHEN** calculating participant balances
**THEN** owed and contributed amounts are accurate

### Requirement: Payment Provider Integration
The system MUST integrate with external payment providers via PaymentProvider interface.

#### Scenario: Tricount Integration
**WHEN** using Tricount provider
**THEN** group creation, expense addition, and data export work correctly

#### Scenario: Mock Provider
**WHEN** using mock provider for testing
**THEN** all operations return appropriate mock data

## MODIFIED Requirements
None at this time.

## REMOVED Requirements
None at this time.

## Data Models

### Pot
- id: String (generated)
- eventId: String
- organizerId: String
- goalAmount: Double
- currentAmount: Double
- currency: String
- title: String
- status: PotStatus
- paymentProvider: PaymentProvider
- tricountGroupId: String?
- tricountGroupUrl: String?
- createdAt: String
- closedAt: String?

### Contribution
- id: String (generated)
- potId: String
- participantId: String
- amount: Double
- description: String
- paidBy: String
- sharedBy: List<String>
- paidAt: String

### Enums
- PotStatus: ACTIVE, CLOSED_SUCCESS, CLOSED_INCOMPLETE
- PaymentProvider: TRICOUNT, PAYPAL, STRIPE, REVOLUT, KNOT
- TricountCategory: TRANSPORT, ACCOMMODATION, FOOD, ACTIVITIES, EQUIPMENT, MISCELLANEOUS

## API Integration

### Tricount API
- Create group: POST /groups
- Add expense: POST /groups/{id}/expenses
- Get group link: GET /groups/{id}/link
- Export data: GET /groups/{id}/export

### Error Handling
- EventNotFoundException
- NoActivePotException
- PotNotFoundException
- TricountNotConfiguredException
- PaymentProviderException

## Security Considerations
- Payment data encryption
- Secure token storage for provider APIs
- Input validation for amounts and currencies
- Audit logging for all payment operations