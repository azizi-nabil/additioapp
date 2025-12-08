# TeacherHub v3.3.0 Changelog

**Release Date:** December 8, 2024

## New Features

### Grade Entry Page Improvements
- **Min/Max Mark Filter**: Added filter row with Min/Max inputs to filter students by mark range
  - Enter min only → shows marks >= min
  - Enter max only → shows marks <= max
  - Enter both → shows marks between min and max (inclusive)
  - Clear button to reset filters

- **Mark Validation**: Input validation for grade marks
  - Rejects values greater than max score (shows "Max: X" error)
  - Rejects non-numeric input (shows "Invalid" error)
  - Blank entries are allowed and save correctly

- **Assignment Icon**: Added assignment icon before grade item title

- **Blank Marks Fix**: Blank marks now save correctly (stored as -1, displayed as empty)

- **Not Graded Filter Fix**: "Not Graded" filter now correctly includes students with blank marks

### Grades Page
- **Assessment Count**: Shows "X Assessments" on top left
- **FAB Toggle**: Eye button moved here to control visibility of "Add Grade Item" FAB

### Simplified Sort Across All Pages
Applied to: **Students**, **Behavior**, **Grade Entry**

- Sort options simplified to: **A-Z**, **Z-A**, **ID**
- Sort uses **settings preference** to determine whether A-Z/Z-A sorts by lastname or firstname
- Button displays: "Sort: A-Z", "Sort: Z-A", or "Sort: ID"

## Technical Changes

### Files Modified
- `GradeEntryFragment.kt` - Min/max filter, sort simplification, blank marks handling
- `GradeEntryAdapter.kt` - Mark validation, blank display logic
- `StudentsFragment.kt` - Sort simplification with settings preference
- `BehaviorFragment.kt` - Sort simplification with settings preference
- `GradesFragment.kt` - Assessment count, FAB toggle
- `fragment_grade_entry.xml` - Min/max filter row, assignment icon
- `fragment_grades.xml` - Assessment count TextView
- `item_grade_entry.xml` - Mark label (was Score)
- `strings.xml` - Added sort_az, sort_za, sort_display_* strings
- `build.gradle.kts` - Version bump to 3.3.0

### Version Info
- **versionCode**: 10
- **versionName**: 3.3.0
