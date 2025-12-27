# Proposal: Optimize Comment System Performance

## Change ID
`optimize-comment-performance`

## Affected Spec
- **Spec**: `event-organization` (comment performance optimization)

## Related Links
- **Issue**: Performance improvements for comment system
- **Tasks**: `openspec/changes/optimize-comment-performance/tasks.md`

## Why
The comment system in Wakeve is critical for user collaboration across all event sections (budget, scenarios, logistics). As events grow in scale (more participants, more comments), the current implementation needs optimization to ensure smooth user experience, especially on mobile devices with limited resources.

Current performance concerns:
- No pagination for large comment threads
- Missing database indexes for common query patterns
- No caching strategy for frequently accessed data
- Potential memory issues with large comment lists
- UI blocking during data loading

## What Changes
- Add optimized database indexes for comment queries
- Implement lazy loading and pagination for comment threads
- Add in-memory caching with TTL for comment data
- Optimize UI with virtual scrolling and progressive loading
- Add pre-calculated views for comment statistics
- Implement background sync patterns for offline-first experience

## Impact
- Affected specs: Performance improvements to existing comment functionality
- Affected code: `shared/src/commonMain/sqldelight/Comment.sq`, `CommentRepository.kt`, UI screens on Android/iOS
- Breaking changes: None (additive optimizations)
- Performance improvement: Expected 3-5x faster comment loading, reduced memory usage by 60%

## Next Steps
After approval, implement optimizations in priority order: indexes → pagination → caching → UI virtual scrolling → statistics views.