# Performance Tests Quick Reference

## Location
`shared/src/commonTest/kotlin/com/guyghost/wakeve/performance/AgentPerformanceTest.kt`

## Quick Start

### Run all performance tests
```bash
./gradlew shared:jvmTest
```

### Run performance tests only
```bash
./gradlew shared:jvmTest -k AgentPerformance
```

## Test Coverage

| # | Test Name | Component | Threshold | Status |
|---|-----------|-----------|-----------|--------|
| 1 | testRecommendationEngineLatency | RecommendationEngine | 100ms | ✅ |
| 2 | testSuggestionServiceScalability | SuggestionService | 1s | ✅ |
| 3 | testTransportServiceOptimization | TransportService | 150-200ms | ✅ |
| 4 | testTransportServiceLoadTest | TransportService | 300ms | ✅ |
| 5 | testDatabaseQueryOptimization | EventRepository | 100ms | ✅ |
| 6 | testConcurrentAccessStress | Repository | No errors | ✅ |
| 7 | testMemoryAllocationE2E | Full workflow | 50MB | ✅ |
| 8 | testLargeDatasetHandling | Batch operations | 1s | ✅ |
| 9 | testNetworkLatencyTolerance | Service resilience | 5s timeout | ✅ |
| 10 | testColdStartPerformance | App launch | 1.5s | ✅ |

## Key Metrics

### Latency (ms)
- RecommendationEngine: < 100ms
- SuggestionService: < 1000ms
- TransportService (Cost): < 200ms
- TransportService (Time): < 150ms
- TransportService (Balanced): < 180ms
- Database Queries: < 100ms (events), < 50ms (participants)
- Cold Start: < 1500ms

### Memory
- Peak allocation: < 50MB
- Per-operation: < 10MB
- GC pauses: < 10ms

### Concurrency
- Thread-safety: No race conditions
- Data integrity: No corruption
- Scalability: Linear performance

## Test Data Scales

- User preferences: 100
- Participants: 10-100
- Events: 50-100
- Scenarios: 10
- Transport locations: 5-20
- Concurrent users: 10

## Helper Functions

```kotlin
// Scenario generation
generateTestScenarios(count: Int): List<Scenario>

// User preferences
generateTestPreferences(count: Int): List<SuggestionUserPreferences>

// Event data
generateEvents(organizerId, count, participantsPerEvent): List<Event>
generateTestEvent(eventId, participantCount): Event

// Participant IDs
generateParticipantIds(count: Int): List<String>
```

## Performance Assertions

```kotlin
// Latency testing
assertTrue(
    actual = elapsedTime < 100,
    message = "Should complete in < 100ms, got ${elapsedTime}ms"
)

// Memory testing
val memoryUsedMB = memoryUsed / (1024 * 1024)
assertTrue(
    actual = memoryUsed < 50_000_000,
    message = "Memory usage should be < 50MB, used ${memoryUsedMB}MB"
)

// Concurrency testing
assertTrue(
    actual = successCount == participantIds.size,
    message = "All operations should succeed"
)
```

## AAA Pattern

All tests follow this structure:

```kotlin
// ARRANGE - Setup test data
val testData = generateTestData()

// ACT - Execute the operation
val startTime = System.currentTimeMillis()
val result = service.doSomething(testData)
val elapsedTime = System.currentTimeMillis() - startTime

// ASSERT - Verify results
assertTrue(elapsedTime < THRESHOLD, "Performance check")
assertTrue(result.isValid(), "Functional check")
```

## Debugging Tips

### Check latency
- Use `System.currentTimeMillis()` for measurements
- Run tests multiple times for consistency
- Check for background tasks affecting results

### Check memory
- Call `System.gc()` between tests
- Use `Runtime.getRuntime()` for memory info
- Watch for memory leaks in profiler

### Check concurrency
- Review thread safety assertions
- Check for duplicate detection
- Verify final state consistency

## Integration with CI/CD

These tests are automatically run as part of:
```
./gradlew build
./gradlew shared:jvmTest
```

Add to your CI pipeline:
```yaml
- name: Run Performance Tests
  run: ./gradlew shared:jvmTest -k AgentPerformance
```

## Monitoring Performance

Create baseline from first run:
```bash
./gradlew shared:jvmTest -k AgentPerformance 2>&1 | tee baseline.log
```

Compare against baseline:
```bash
./gradlew shared:jvmTest -k AgentPerformance 2>&1 | diff baseline.log -
```

## Performance Thresholds

These are based on UX requirements:
- **< 100ms**: Imperceptible (instant)
- **< 300ms**: No perception of delay
- **< 1000ms**: User stays focused
- **> 5000ms**: Will cause timeout

Adjust thresholds based on actual UX requirements.

## Future Optimizations

Priority order for improvements:
1. Database query batching (N+1 elimination)
2. Caching frequently accessed data
3. Algorithm optimization
4. Async processing for heavy operations
5. Memory pooling for large object allocations

## References

- Architecture: `/docs/architecture/README.md`
- Testing Guide: `/docs/testing/README.md`
- OpenSpec Changes: `/openspec/changes/`
- Commit: `6660452`

## Contact

For questions about performance tests:
- Review the test documentation in the test file
- Check implementation files for optimization opportunities
- Profile with IDE tools (IntelliJ Profiler, etc.)
