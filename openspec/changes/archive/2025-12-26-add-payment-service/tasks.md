# Add Payment Service Tasks

## Database Schema Updates
- [x] Add Pot table to SQLDelight schema
- [x] Add Contribution table to SQLDelight schema
- [x] Update WakevDb with new queries

## Models and DTOs
- [x] Implement Pot, Contribution, ParticipantBalance models
- [x] Implement PotBalance, TricountLink, TricountExport models
- [x] Implement PaymentStatistics and related enums

## PaymentProvider Interface
- [x] Define PaymentProvider interface
- [x] Implement MockPaymentProvider for testing
- [x] Define TricountCategory and PaymentProvider enums

## PaymentService Implementation
- [x] Implement PaymentService class with all methods
- [x] Add createPot functionality
- [x] Add addContribution functionality
- [x] Add getPotBalance functionality
- [x] Add closePot functionality
- [x] Add generateTricountLink functionality
- [x] Add generatePotLink functionality
- [x] Add exportTricountData functionality
- [x] Add updatePotDetails functionality
- [x] Add getPaymentStatistics functionality

## Helper Functions
- [x] Implement calculateParticipantBalances
- [x] Implement generatePotId and generateContributionId
- [x] Add notifyPotClosed private function

## Integration Points
- [x] Integrate with BudgetRepository
- [x] Integrate with NotificationService
- [x] Update EventRepository if needed

## Tests
- [x] Write PaymentServiceTest with all test cases
- [x] Test createPot creates Tricount pot
- [x] Test addContribution updates pot balance
- [x] Test getPotBalance calculates correct amounts
- [x] Test closePot updates status correctly
- [x] Test generateTricountLink returns valid link
- [x] Test calculateParticipantBalances is accurate

## Error Handling
- [x] Define custom exceptions (EventNotFoundException, etc.)
- [x] Add proper Result handling throughout

## Validation
- [x] Validate proposal with openspec validate
- [ ] Run all tests (36+ tests should pass)
- [ ] Check offline functionality
- [ ] Review code for design system compliance