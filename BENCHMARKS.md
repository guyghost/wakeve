# Wakeve Performance Benchmarks

This document contains performance benchmarks for the Wakeve application to ensure the app meets performance targets and provides a smooth user experience.

## Performance Targets

| Metric | Target | Measured | Status |
|--------|--------|----------|--------|
| App startup time | < 2s | TBD | 🔄 |
| Event list load time (50 items) | < 100ms | TBD | 🔄 |
| Event creation time | < 500ms | TBD | 🔄 |
| Vote submission time | < 200ms | TBD | 🔄 |
| Memory usage (idle) | < 100MB | TBD | 🔄 |

## Benchmark Categories

### 1. App Startup Benchmark

**Purpose**: Measure the time taken for the application to initialize and become usable.

**What's measured**:
- Repository initialization
- Initial data loading
- Core service setup

**Test Method**:
```kotlin
// Simulate app startup by creating repository and loading initial data
val repo = EventRepository()
repo.getAllEvents()
```

**Target**: < 2000ms average across 10 iterations

### 2. Event List Load Benchmark

**Purpose**: Measure the time to load and display a list of events.

**What's measured**:
- Database query performance
- Pagination efficiency
- Sorting and filtering

**Test Method**:
```kotlin
// Load 50 events with pagination
repository.getEventsPaginated(
    page = 0,
    pageSize = 50,
    orderBy = OrderBy.CREATED_AT_DESC
).first()
```

**Target**: < 100ms for 50 items

### 3. Event Creation Benchmark

**Purpose**: Measure the time to create a new event.

**What's measured**:
- Event validation
- Database insertion
- Poll creation

**Test Method**:
```kotlin
// Create event with time slots
val event = createTestEvent(...)
repository.createEvent(event)
```

**Target**: < 500ms average

### 4. Vote Submission Benchmark

**Purpose**: Measure the time to submit a single vote.

**What's measured**:
- Vote validation
- Database update
- Poll recalculation

**Test Method**:
```kotlin
// Add vote for participant
repository.addVote(eventId, participantId, slotId, Vote.YES)
```

**Target**: < 200ms per vote

### 5. Memory Usage Benchmark

**Purpose**: Measure memory consumption during typical app usage.

**What's measured**:
- Initial memory footprint
- Memory growth with operations
- Memory efficiency

**Test Method**:
```kotlin
// Create and manipulate 100 events
// Measure memory before and after
System.gc()
val memoryUsed = (finalMemory - initialMemory) / (1024 * 1024)
```

**Target**: < 100MB total memory usage

### 6. Complex Workflow Benchmark

**Purpose**: Measure performance of complete user workflows.

**What's measured**:
- End-to-end operation time
- Multiple sequential operations
- Real-world usage patterns

**Test Method**:
```kotlin
// Complete workflow: create → poll → vote → confirm
repository.createEvent(event)
repository.updateEventStatus(..., POLLING, ...)
repository.addVote(...)
repository.updateEventStatus(..., CONFIRMED, ...)
```

**Target**: < 1000ms for complete workflow

### 7. Concurrent Operations Benchmark

**Purpose**: Measure performance under concurrent load.

**What's measured**:
- Thread safety overhead
- Concurrent database access
- Lock contention

**Test Method**:
```kotlin
// Simulate multiple concurrent users
val jobs = (1..10).map { async { /* concurrent operations */ } }
jobs.forEach { it.await() }
```

**Target**: Efficient processing < 5 seconds for 20 events

## Running Benchmarks

### Command Line

```bash
# Run all performance benchmarks
./gradlew shared:test --tests "*PerformanceBenchmarks*"

# Run specific benchmark
./gradlew shared:test --tests "*PerformanceBenchmarks.benchmarkAppStartup*"

# Run with detailed output
./gradlew shared:test --tests "*PerformanceBenchmarks*" --info
```

### Running Benchmarks in IDE

1. Open the `PerformanceBenchmarks.kt` file
2. Right-click on the test class or individual test method
3. Select "Run 'PerformanceBenchmarks'"
4. View results in the console output

## Understanding Results

### Output Format

Each benchmark produces output like:

```
=== Event List Load Benchmark ===
Event count: 50
Average load time: 45.2ms
Max load time: 67ms
Target: < 100ms for 50 items
```

### Performance Indicators

- ✅ **PASS**: Metric meets or exceeds target
- ❌ **FAIL**: Metric does not meet target
- 🔄 **TBD**: To be determined (first run)

### Summary Report

At the end of all benchmarks, a summary is printed:

```
=== BENCHMARK SUMMARY ===
Total benchmarks: 7
Passed: 6
Failed: 1
Success rate: 86%

❌ Failed Benchmarks:
- Vote submission: 245ms (target: 200ms)
```

## Benchmark Environment

### Test Configuration

- **Platform**: JVM (for consistency)
- **Database**: In-memory SQLite (via EventRepository mock)
- **Data Size**: Controlled test datasets
- **Iterations**: Multiple runs for statistical significance

### Factors Affecting Performance

1. **Hardware**: CPU speed, memory, storage I/O
2. **Data Size**: Number of events, participants, time slots
3. **Network**: Latency for remote operations (not in current benchmarks)
4. **Concurrent Load**: Number of simultaneous users

## Performance Optimization Guidelines

### When Benchmarks Fail

1. **Analyze the bottleneck**: Use profiling tools to identify slow code
2. **Review data structures**: Ensure efficient algorithms are used
3. **Optimize database queries**: Add indexes, avoid N+1 queries
4. **Implement caching**: Cache frequently accessed data
5. **Reduce memory allocations**: Reuse objects, minimize temporary allocations

### Continuous Performance Monitoring

1. **Run benchmarks on every PR**: Include in CI/CD pipeline
2. **Set performance budgets**: Fail build if metrics degrade
3. **Track performance over time**: Monitor trends and regressions
4. **Profile regularly**: Use profiling tools to identify opportunities

## Platform-Specific Considerations

### Android

- **Cold start vs. warm start**: Different optimization strategies
- **Memory pressure**: Android's aggressive memory management
- **Battery optimization**: Minimize CPU usage for better battery life

### iOS

- **Launch time**: App Store review requirements (< 3 seconds)
- **Memory limits**: iOS memory limits per device
- **Background execution**: iOS background execution policies

### Web (Future)

- **Initial bundle size**: JavaScript bundle size affects load time
- **Network latency**: Consider CDN and asset optimization
- **Browser performance**: Varying performance across browsers

## Historical Performance Data

| Date | Version | App Startup | Event List | Event Creation | Vote Submission | Memory Usage |
|------|---------|-------------|------------|----------------|-----------------|--------------|
| TBD | v1.0.0 | TBD | TBD | TBD | TBD | TBD |

*This section will be populated as benchmarks are run over time*

## Performance Regression Detection

### Alert Thresholds

- **Critical degradation**: > 50% slower than baseline
- **Warning**: > 20% slower than baseline
- **Improvement**: > 10% faster than baseline

### Baseline Establishment

The first successful benchmark run establishes the baseline for future comparisons.

## Contributing

When contributing code that may affect performance:

1. **Run benchmarks locally** before submitting PR
2. **Include benchmark results** in PR description
3. **Explain any performance changes** (improvements or regressions)
4. **Update optimization strategies** in this document if needed

## References

- [Android Performance Guidelines](https://developer.android.com/topic/performance)
- [iOS Performance Best Practices](https://developer.apple.com/documentation/xcode/improving_your_app_s_performance)
- [Kotlin Performance Tips](https://kotlinlang.org/docs/performance.html)
- [SQLDelight Performance](https://cashapp.github.io/sqldelight/performance/)

---

*Last updated: 2025-02-11*  
*Version: 1.0.0*