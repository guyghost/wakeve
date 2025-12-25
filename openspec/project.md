# Project Context

## Purpose
Wakeve is a Kotlin Multiplatform mobile application designed for collaborative event planning between friends and small groups.
It supports a wide range of events (trips, dinners, birthdays, surprise events, weddings) and streamlines the entire process from early ideation and date polling to full logistical organization, including transport, lodging, food, equipment, and shared budgets.

The product strongly emphasizes:
- collective decision-making,
- budget transparency,
- task responsibility sharing,
- and a seamless offline-first user experience.

## Tech Stack
- Kotlin Multiplatform (for shared domain and application logic)
- Jetpack Compose for the Android UI
- SwiftUI for the iOS UI
- Ktor for both the client (shared) and a backend proxy server
- SQLDelight for local database storage (SQLite)

## Project Conventions

### Code Style
Our code style prioritizes clarity, consistency, and automation across all languages used in the project.

- **Kotlin (Shared, Android, Backend):** We adhere strictly to the official Kotlin coding conventions. The `ktlint` tool is integrated into the build process to automatically enforce formatting and style rules. Key principles include:
    - Preferring expressions over statements.
    - Using `val` wherever possible.
    - Writing idiomatic code by leveraging the standard library.
    - For Jetpack Compose, we follow the official Compose API guidelines, such as naming Composable functions with a capital letter and making them idempotent.

- **Swift (iOS):** We follow Apple's official Swift API Design Guidelines. `SwiftLint` is used to enforce these conventions. The primary goal is clarity at the point of use.

- **SQL (SQLDelight):** Queries written in `.sq` files should be clear and readable. Table and column names use `snake_case`.

- **General Principle:** All code, regardless of language, should be self-documenting. Use meaningful names for variables, functions, and classes. Add comments only to explain complex logic or the "why" behind a decision, not the "what".

### Architecture Patterns
- **Shared Logic:** Core domain and application logic are written in a shared Kotlin Multiplatform module.
- **Backend Proxy:** A Ktor-based backend is used as a proxy to handle external API calls and aggregate data, keeping API keys and complex logic off the clients.
- **Provider Model:** The architecture uses abstract providers for services like Destination, Lodging, and Transport. This allows for mock implementations during development and easy swapping for real implementations later.
- **Local-First Sync:** The application uses SQLDelight for a local source of truth. Data is synchronized incrementally with the backend.
- **Conflict Resolution:** The current conflict resolution strategy is "last-write-wins" based on timestamps, with plans to evolve to a CRDT-based model.
- **UI:** The UI is built natively on each platform, with Jetpack Compose on Android and SwiftUI on iOS, interoperating with the shared KMP framework.
- **Scenario-based Planning:** Before confirmation, multiple scenarios (date + location + estimated cost) can coexist and be compared.
- **Local-First Sync:** SQLDelight remains the source of truth, including polls, scenarios, logistics, and budget estimations.

### Testing Strategy
The project follows a Test-Driven Development (TDD) approach. New features and bug fixes should begin with writing failing tests that describe the desired functionality or behavior, followed by the implementation that makes the tests pass. All code should be thoroughly unit-tested, and integration tests are used to verify interactions between different agents and components.

### Git Workflow
We use a Trunk-Based Development model. All developers commit directly to a single branch, `main`. To keep the main branch stable and always releasable, development of significant features is managed through feature flags. Code is expected to be integrated frequently in small, incremental commits.

## Domain Context
The application revolves around Events as the core domain object. Events evolve through clearly defined phases and are coordinated by both human and software agents.

### **Event Lifecycle**

**1. Idea / Draft**
- Event created
- Participants invited

**2. Polling**
- Multiple dates or periods proposed
- Participants vote (Yes / Maybe / No)

**3. Scenario Comparison**
- Optional shortlist of scenarios
- Each scenario includes:
  - date or period
  - destination
  - duration
  - estimated number of participants
  - approximate budget per person

**4. Confirmed**
- A single date (and optionally scenario) is locked by the organizer

**5. Organization**
- Detailed planning of logistics:
  - transport
  - lodging
  - food
  - equipment & activities
  - shared costs

**6. Finalized**
- All critical information confirmed
- Event ready for execution

### **Human Agents:**
- **Organizer**
  - Creates and configures the event
  - Proposes dates and scenarios
  - Sets deadlines
  - Confirms the final date
  - Oversees logistics and budget

- **Participant**
  - Votes on dates and scenarios
  - Confirms attendance
  - Provides departure location
  - Contributes to logistics, food, and equipment once the date is confirmed


### **Software Agents**

- **Poll & Calendar Agent**
  - Manages time slots, votes, deadlines, and timezones
  - Computes the best possible date or period

- **Scenario & Budget Agent**
  - Manages scenario shortlists
  - Aggregates estimated costs (transport, lodging, food)
  - Computes per-person budget approximations

- **Destination & Lodging Agent**
  - Suggests destinations and accommodations based on scenarios

- **Transport Agent**
  - Recommends travel options based on participant locations and confirmed dates

- **Food & Equipment Agent**
  - Manages meal planning, dietary constraints

- **Handles collaborative equipment checklists**

- **Payment & Tricount Agent**
  - Tracks shared expenses
  - Integrates with Tricount for settlement

- **Sync & Offline Agent**
  - Handles offline-first storage
  - Queues actions and resolves conflicts (last-write-wins for now)

- **Security & Auth Agent**
  - Manages authentication, permissions, and access control
  - 
- **Notifications Agent**
  - Sends poll reminders, confirmations, and organizational updates


### General Workflow

1. Organizer creates an event
2. Dates or periods are proposed
3. Participants vote
4. Optional scenarios are created and compared
5. Poll Agent recommends the optimal date
6. Organizer confirms the date
7. Logistics and budget planning are unlocked
8. Only confirmed participants can access detailed organization data

## Important Constraints
- **Timezone Consistency:** The system must handle timezones correctly for all users across polling and deadlines.
- **Offline Transparency:** The UI must clearly communicate when the user is offline and if their actions are queued for later synchronization.
- **Security:** External links (e.g., for payments or meetings) must be handled securely to prevent phishing.
- **GDPR Compliance:** The application must manage user consent, data minimization, and provide options for data export and deletion.
- **Access Control:** Full event details are only visible to participants who have confirmed their attendance for the final date.

## External Dependencies
- **Authentication:** OAuth via Google and Apple.
- **Meetings:** Generation of links for Zoom, Google Meet, and FaceTime.
- **Payments:** Integration with external payment providers for shared pools and cost tracking via Tricount.
