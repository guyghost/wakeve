# Add Payment Service

## Why
Wakeve needs a payment management system to handle event cagnottes (pots) and integrate with Tricount for cost sharing. This is essential for Phase 4 features to enable full event organization with budget tracking and payment coordination.

## What
Implement PaymentService to:
- Create and manage cagnottes for events
- Handle participant contributions
- Integrate with Tricount for expense tracking
- Calculate balances and statistics
- Support multiple payment providers (Tricount, PayPal, Stripe, etc.)

## Impact
- Adds payment functionality to events
- Enables cost sharing and budget management
- Integrates with external payment services
- Requires new database tables for pots and contributions
- Updates existing event and participant models

## Dependencies
- SQLDelight schema updates for pot and contribution tables
- BudgetRepository integration
- NotificationService for pot updates
- External API integrations (Tricount, etc.)

## Risks
- External API dependencies (Tricount, PayPal)
- Data privacy with payment information
- Currency handling and exchange rates
- Legal compliance for payments