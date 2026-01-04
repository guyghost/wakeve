# DRAFT Workflow - Diagrammes

## Diagramme de Séquence - Création DRAFT

```
┌─────────────┐      ┌─────────────┐      ┌──────────────┐      ┌──────────────┐
│    User     │      │  UI Wizard   │      │State Machine  │      │  Repository   │
└──────┬──────┘      └──────┬──────┘      └──────┬───────┘      └──────┬───────┘
       │                    │                    │                     │
       │ Start Creation    │                    │                     │
       │──────────────────>│                    │                     │
       │                    │                    │                     │
       │ Fill Step 1       │                    │                     │
       │──────────────────>│                    │                     │
       │                    │                    │                     │
       │ Click "Next"      │                    │                     │
       │──────────────────>│                    │                     │
       │                    │                    │                     │
       │                    │ UpdateDraftEvent  │                     │
       │                    │───────────────────>│                     │
       │                    │                    │                     │
       │                    │                    │ update(event)         │
       │                    │                    │─────────────────────>│
       │                    │                    │                     │
       │                    │                    │◄────────────────────┤
       │                    │ Auto-save OK     │                     │
       │                    │<───────────────────│                     │
       │                    │                    │                     │
       │ Show Step 2        │                    │                     │
       │<───────────────────│                    │                     │
       │                    │                    │                     │
       │ Fill Step 2       │                    │                     │
       │──────────────────>│                    │                     │
       │                    │                    │                     │
       │ ... (repeat for steps 3-4)           │                     │
       │                    │                    │                     │
       │ Click "Create"    │                    │                     │
       │──────────────────>│                    │                     │
       │                    │                    │                     │
       │                    │ CreateEvent       │                     │
       │                    │───────────────────>│                     │
       │                    │                    │                     │
       │                    │                    │ insert(event)         │
       │                    │                    │─────────────────────>│
       │                    │                    │                     │
       │                    │                    │◄────────────────────┤
       │                    │ Event Created     │                     │
       │                    │<───────────────────│                     │
       │                    │                    │                     │
       │                    │ NavigateTo("detail/{id}")             │
       │                    │<───────────────────│                     │
       │                    │                    │                     │
       │ Navigate to Detail │                    │                     │
       │<───────────────────│                    │                     │
       │                    │                    │                     │
```

## Diagramme de Flux - Navigation DRAFT

```mermaid
graph TD
    A[EventListScreen] -->|User taps Create Event| B[DraftEventWizard Step1]
    B -->|User fills + Next| C{Validate Step1}
    C -->|Valid| D[Auto-save Step1]
    C -->|Invalid| B
    D --> E[DraftEventWizard Step2]
    E -->|User fills + Next| F{Validate Step2}
    F -->|Valid| G[Auto-save Step2]
    F -->|Invalid| E
    G --> H[DraftEventWizard Step3]
    H -->|User adds locations + Next| I{Validate Step3}
    I -->|Valid| J[Auto-save Step3]
    I -->|Invalid| H
    J --> K[DraftEventWizard Step4]
    K -->|User adds slots + Create| L{Validate Step4}
    L -->|Valid| M[Auto-save Step4 + CreateEvent]
    L -->|Invalid| K
    M --> N[Event Created]
    N --> O[EventDetailScreen]
    O -->|User taps Start Poll| P[StartPoll Intent]
    P --> Q[Event Status: POLLING]
    Q --> R[PollSetupScreen]

    B -->|User clicks Cancel| S[Discard Data]
    S --> A
```

## Diagramme d'États - Workflow DRAFT

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Event Status: DRAFT                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              WIZARD STEP STATE MACHINE                      │   │
│   ├─────────────────────────────────────────────────────────────┤   │
│   │                                                             │   │
│   │  ┌──────────┐     ┌──────────┐     ┌──────────┐         │   │
│   │  │  STEP 1  │────>│  STEP 2  │────>│  STEP 3  │         │   │
│   │  │Basic Info │     │Particpts │     │Locations │         │   │
│   │  └────┬─────┘     └────┬─────┘     └────┬─────┘         │   │
│   │       │                  │                  │                 │   │
│   │       │ Valid            │ Valid            │ Valid          │   │
│   │       │ Auto-save        │ Auto-save        │ Auto-save      │   │
│   │       │                  │                  │                 │   │
│   │       └──────────────────┴──────────────────┘                 │   │
│   │                           │                                 │   │
│   │                           ▼                                 │   │
│   │                    ┌──────────┐                            │   │
│   │                    │  STEP 4  │                            │   │
│   │                    │Time Slots│                            │   │
│   │                    └────┬─────┘                            │   │
│   │                         │                                   │   │
│   │                         │ Valid + Create                     │   │
│   │                         ▼                                   │   │
│   │                    ┌──────────────┐                        │   │
│   │                    │ EVENT CREATED│                        │   │
│   │                    │ NavigateTo   │                        │   │
│   │                    └──────────────┘                        │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Diagramme de Flux d'Erreur

```
┌─────────────┐
│  User       │
└──────┬──────┘
       │
       │ Fill Step with Invalid Data
       │
       ▼
┌─────────────┐
│  UI Wizard  │
└──────┬──────┘
       │
       │ Trigger Validation
       │
       ▼
┌─────────────────────────────────────┐
│  Validation Logic                 │
│  - title.isEmpty()?              │
│  - description.isEmpty()?         │
│  - (maxParticipants < min)?       │
│  - timeSlots.isEmpty()?          │
└──────┬──────────────────────────┘
       │
       │ Invalid
       │
       ├─────────────────────────────┐
       │                           │
       ▼                           ▼
┌─────────────┐           ┌─────────────┐
│ Show Error  │           │ Disable     │
│ (Toast)     │           │ Next Button │
└──────┬──────┘           └─────────────┘
       │
       ▼
┌─────────────┐
│ User fixes  │
│ the error   │
└──────┬──────┘
       │
       │ Retry Validation
       │
       ▼
┌─────────────┐
│ Valid →     │
│ Enable Next │
└─────────────┘
```

## Mapping Intents - UI ↔ State Machine

| UI Component | User Action | Intent Dispatched | Side Effect |
|-------------|-------------|-------------------|-------------|
| DraftEventWizard (Step 1) | Fill & Click Next | `UpdateDraftEvent` | Auto-save + NavigateTo Step 2 |
| DraftEventWizard (Step 2) | Fill & Click Next | `UpdateDraftEvent` | Auto-save + NavigateTo Step 3 |
| DraftEventWizard (Step 3) | Add Location | `AddPotentialLocation` | Auto-save |
| DraftEventWizard (Step 3) | Remove Location | `RemovePotentialLocation` | Auto-save |
| DraftEventWizard (Step 3) | Click Next | (optional auto-save) | NavigateTo Step 4 |
| DraftEventWizard (Step 4) | Add TimeSlot | `AddTimeSlot` | Auto-save |
| DraftEventWizard (Step 4) | Remove TimeSlot | `RemoveTimeSlot` | Auto-save |
| DraftEventWizard (Step 4) | Click Create | `CreateEvent` | NavigateTo("detail/{id}") |
| DraftEventWizard | Click Cancel | - | NavigateBack |
