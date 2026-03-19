## MODIFIED Requirements

### Requirement: Repository Artifacts Stay Consistent With Runtime Modules
The project MUST keep documentation, build commands, and module path references consistent with the actual Kotlin Multiplatform module layout in the repository.

#### Scenario: Documentation references an outdated module name
- **WHEN** a contributor follows setup/build instructions
- **THEN** all referenced module names and paths exist in the repository
- **AND** the documented build commands execute against real modules without manual translation

#### Scenario: Source tree contains stale backup artifacts
- **WHEN** maintainers review tracked source files
- **THEN** backup/disabled artifacts are either removed from active source trees or explicitly justified in contributor documentation
- **AND** canonical implementations are clearly identifiable by package and path

