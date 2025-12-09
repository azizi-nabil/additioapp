# Changelog v3.3.3

**Release Date:** December 9, 2025

## Bug Fixes

### Attendance Module - Duplicate Session Prevention

Fixed critical bugs that allowed creating duplicate attendance sessions for the same date and session type.

#### Issues Fixed:

1. **Duplicate Session Detection**
   - Fixed save check to properly detect existing sessions by exact sessionId
   - Prevents creating two Cours/TD/TP sessions on the same date

2. **Startup Deduplication**
   - Fixed cleanup logic to group by `(studentId, sessionId)` instead of `(studentId, date)`
   - Now correctly preserves different session types on the same date (Cours + TD + TP)

3. **Spinner Type Change Behavior**
   - Fixed issue where switching spinner type while viewing an existing session didn't enable new session mode
   - Cleared `originalSessionId` on type change to ensure correct sessionId is used
   - Now properly loads existing records when switching to a type that has data

4. **Session Type Preservation**
   - Fixed bug where switching between types and back caused session deletion
   - Each session type now maintains its own records independently

#### Expected Behavior:

- ✅ Create Cours, TD, and TP sessions on the same date (all three types allowed)
- ✅ Switch between types without data loss
- ✅ Exit and re-enter without losing data
- ❌ Create two Cours sessions on the same date (blocked with warning)

## Files Modified

- `AttendanceFragment.kt` - Save validation, spinner listener, session detection logic
- `AppRepository.kt` - Fixed deduplication grouping
- `AttendanceDao.kt` - Added pattern query methods
- `AttendanceViewModel.kt` - Added sync save method
- `AdditioApplication.kt` - Added startup cleanup
