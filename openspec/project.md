# Project Context

## Purpose
Wakeve is a Kotlin Multiplatform mobile application designed for collaborative event planning. It streamlines the process from polling for availability to finalizing details like destination, lodging, and transport, with a strong emphasis on providing a seamless offline-first user experience.

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

### Testing Strategy
The project follows a Test-Driven Development (TDD) approach. New features and bug fixes should begin with writing failing tests that describe the desired functionality or behavior, followed by the implementation that makes the tests pass. All code should be thoroughly unit-tested, and integration tests are used to verify interactions between different agents and components.

### Git Workflow
We use a Trunk-Based Development model. All developers commit directly to a single branch, `main`. To keep the main branch stable and always releasable, development of significant features is managed through feature flags. Code is expected to be integrated frequently in small, incremental commits.

## Domain Context
The application revolves around "Agents" (both human and software) that collaborate to plan an event.

- **Human Agents:**
  - **Organizer:** Creates events, sets up polls for dates, defines deadlines, and validates the final chosen date.
  - **Participant:** Votes on proposed slots, can suggest new slots if permitted, and contributes to event details after a date is confirmed.

- **Software Agents:** The system is composed of several specialized agents:
  - **Poll & Calendar Agent:** Manages time slots, votes, timezones, and calculates the best slot.
  - **Destination & Lodging Agent:** Suggests destinations and accommodations.
  - **Transport Agent:** Recommends travel options based on participant locations.
  - **Meetings Agent:** Generates links for virtual meetings (Zoom, Meet).
  - **Payment & Tricount Agent:** Manages shared costs and integrates with Tricount.
  - **Sync & Offline Agent:** Handles local caching, conflict resolution, and network status.
  - **Security & Auth Agent:** Manages authentication and permissions.
  - **Notifications Agent:** Sends reminders and status updates.

The general workflow is:
1. An Organizer creates a poll.
2. Participants vote on time slots.
3. The Poll Agent recommends the best slot, which the Organizer confirms.
4. Once a date is locked, other agents are notified to help plan the rest of the event details (destination, transport, etc.).
5. Access to detailed information is restricted to participants who have confirmed their attendance for the final date.

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
