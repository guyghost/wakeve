## 1. OpenSpec
- [x] 1.1 Add deltas for event organization, scenario management, workflow coordination, destination planning, and offline sync.
- [x] 1.2 Validate the change with `openspec validate add-scenario-matrix-voting --strict`.

## 2. Shared Domain and Persistence
- [x] 2.1 Add planning mode, scenario generation type, source references, and draft scenario status.
- [x] 2.2 Add SQLDelight columns, indexes, and queries for generated matrix scenarios.
- [x] 2.3 Add a pure scenario matrix generation service with deterministic IDs and deduplication support.

## 3. Repository, API, and State Machine
- [x] 3.1 Add repository methods for generating, publishing, and selecting matrix scenarios.
- [x] 3.2 Extend scenario API request/response contracts with source metadata.
- [x] 3.3 Add scenario state-machine intents for generation, publication, and final matrix selection.

## 4. UI and Verification
- [x] 4.1 Surface matrix metadata in existing scenario cards/detail/comparison screens.
- [x] 4.2 Add tests for cartesian generation, idempotent regeneration, publish offline sync metadata, final selection, and legacy compatibility.
- [x] 4.3 Run OpenSpec validation and focused Gradle tests.
