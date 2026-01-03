# 🚨 IMMEDIATE ACTION ITEMS - Phases 1 & 2 Review

**Status**: BLOCKING - Cannot proceed until fixed  
**Time to Fix**: ~30 minutes  
**Priority**: 🔴 CRITICAL

---

## ISSUE #1: Duplicate SQLDelight File 🔴 CRITICAL

### Problem
Two files define the same table:
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/SuggestionPreferences.sq` ✅ KEEP
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/db/SuggestionPreferences.sq` ❌ DELETE

### Quick Fix (< 5 minutes)
```bash
# Step 1: Delete the duplicate file
rm shared/src/commonMain/sqldelight/com/guyghost/wakeve/db/SuggestionPreferences.sq

# Step 2: Verify only one exists
ls -la shared/src/commonMain/sqldelight/com/guyghost/wakeve/*SuggestionPreferences.sq
# Should output only:
# shared/src/commonMain/sqldelight/com/guyghost/wakeve/SuggestionPreferences.sq
```

---

## ISSUE #2: Parameter Count Mismatch in SQL Query 🔴 CRITICAL

### Problem
INSERT query has 17 parameters but table has only 14 columns:
```sql
-- Line 44 in SuggestionPreferences.sq
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         17 parameters - WRONG! Should be 14
```

### Table Structure (14 columns)
```sql
CREATE TABLE suggestion_preferences (
    1. user_id TEXT PRIMARY KEY NOT NULL,
    2. budget_min REAL NOT NULL,
    3. budget_max REAL NOT NULL,
    4. budget_currency TEXT NOT NULL,
    5. preferred_duration_min INTEGER NOT NULL,
    6. preferred_duration_max INTEGER NOT NULL,
    7. preferred_seasons TEXT NOT NULL,
    8. preferred_activities TEXT NOT NULL,
    9. max_group_size INTEGER NOT NULL,
   10. preferred_regions TEXT NOT NULL,
   11. max_distance_from_city INTEGER NOT NULL,
   12. nearby_cities TEXT NOT NULL,
   13. accessibility_needs TEXT NOT NULL,
   14. last_updated TEXT NOT NULL
)
```

### Quick Fix (< 5 minutes)

Edit `shared/src/commonMain/sqldelight/com/guyghost/wakeve/SuggestionPreferences.sq`

**FIND** (line 41-44):
```sql
insertOrReplacePreferences:
INSERT OR REPLACE INTO suggestion_preferences(
    user_id, budget_min, budget_max, budget_currency,
    preferred_duration_min, preferred_duration_max, preferred_seasons,
    preferred_activities, max_group_size, preferred_regions,
    max_distance_from_city, nearby_cities, accessibility_needs, last_updated
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

**REPLACE WITH** (exactly 14 parameters):
```sql
insertOrReplacePreferences:
INSERT OR REPLACE INTO suggestion_preferences(
    user_id, budget_min, budget_max, budget_currency,
    preferred_duration_min, preferred_duration_max, preferred_seasons,
    preferred_activities, max_group_size, preferred_regions,
    max_distance_from_city, nearby_cities, accessibility_needs, last_updated
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

---

## VERIFICATION (< 10 minutes)

### Step 1: Regenerate SQLDelight Interfaces
```bash
./gradlew shared:generateCommonMainWakevDbInterface --rerun-tasks
```
✅ Should complete successfully with no errors

### Step 2: Compile Shared Module
```bash
./gradlew shared:compileCommonMainKotlinMetadata
```
✅ Should complete successfully with no errors

### Step 3: Compile Compose App
```bash
./gradlew composeApp:compileDebugKotlin
```
✅ Should complete successfully with no errors

### Step 4: Run All Tests
```bash
./gradlew shared:jvmTest
```
✅ Should run 60+ tests, all passing

---

## EXPECTED RESULTS AFTER FIX

### Before
```
$ ./gradlew shared:compileCommonMainKotlinMetadata
> Task :shared:generateCommonMainWakevDbInterface FAILED
  - Table already defined with name suggestion_preferences
  - Unexpected number of values being inserted: found: 17 expected: 14
```

### After
```
$ ./gradlew shared:generateCommonMainWakevDbInterface --rerun-tasks
> Task :shared:generateCommonMainWakevDbInterface SUCCESSFUL
  - Generated 1 database interface
  - 0 tables, 0 conflicts
```

---

## NEXT STEPS AFTER FIX

1. ✅ Commit fixes with message:
   ```
   fix(sqldelight): remove duplicate schema and fix parameter count

   - Delete duplicate db/SuggestionPreferences.sq file
   - Fix insertOrReplacePreferences query parameter count (17 → 14)
   - Regenerate SQLDelight interfaces
   - All tests pass (60+)
   ```

2. ✅ Create Pull Request
   - Title: "Fix: SQLDelight schema errors blocking compilation"
   - Description: Include the two fixes above
   - Link to FINAL_REVIEW_PHASES_1_2.md

3. ✅ Merge to main branch once approved

4. ✅ Update tasks.md with completion status

---

## TIMELINE

- **Phase 1**: Fix + Verification (30 minutes)
- **Phase 2**: Tests execution (10 minutes) 
- **Phase 3**: PR review (30-60 minutes)
- **Phase 4**: Merge to main (5 minutes)

**Total Time**: ~2 hours max

---

## RISK ASSESSMENT

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Wrong column count | Low | Critical | Verify table schema matches query |
| Missing columns | Low | Critical | Count columns and parameters manually |
| Cascading failures | Low | Critical | Run verification steps sequentially |
| Test failures | Low | High | Check test error logs |

**Overall Risk**: 🟢 LOW - Fixes are straightforward and well-documented

---

## SUPPORT

If issues occur:
1. Check FINAL_REVIEW_PHASES_1_2.md for detailed analysis
2. Review CORRECTIONS_REQUIRED.md for detailed explanations
3. Check compilation logs for specific errors
4. If stuck, provide error logs to team

---

**Prepared by**: @review  
**Date**: 2026-01-03  
**Urgency**: 🔴 CRITICAL - Blocks all further work  
**Status**: Ready to implement
