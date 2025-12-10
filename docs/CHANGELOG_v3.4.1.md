# Changelog v3.4.1

**Release Date:** December 10, 2024

## 🐛 Bug Fixes

### Grades Report
- **Fixed grade item filtering**: Report now list shows ALL grade items (including calculated ones), except for "CC" and "Exam" categories which remain highlighted at the top.
- **Improved calculated item visibility**: Calculated items in the report list now use the orange sigma icon (Σ) and a soft green background for easier identification.

### Calculated Grades
- **Fixed calculated grades not appearing for all students**: Previously, calculated grade items only showed values for students who already had other grade records. Now all students in the class receive calculated grades immediately.

### Widget
- **Fixed widget not showing today's classes**: Day-of-week calculation was off by 1, causing wrong day's schedule to display.
- **Fixed widget schedule missing class names**: Schedule items now show class name along with time and session type.
- **Fixed widget dividers**: Replaced View with FrameLayout to fix inflation error.

### Tasks
- **Fixed tasks showing only one class**: Tasks with multiple classes now display all class names (comma-separated) in both home screen and widget.

## ✨ New Features

### Grade Entry
- **Sort by Score**: Added ability to sort students by their scores (ascending ↑ or descending ↓)
- **Sort State Display**: The sort button now shows the current sort mode (A-Z, Z-A, ID, Score ↑, Score ↓)

### Widget - Scrollable Sections
- **All sections now scroll**: Schedule, Tasks, Events, and Replacements sections use ListViews
- **Unlimited items**: No longer limited to 3-4 items per section
- **Auto-refresh on app exit**: Widget automatically updates when you leave the app

## 🔧 Improvements

### UI
- Shortened sort option labels for cleaner display (ID, Score ↑, Score ↓)

### Widget Architecture
- New `WidgetService.kt` - RemoteViewsService for collection data
- New `WidgetDataProvider.kt` - Factory providing data for all sections
- New `item_widget_row.xml` - Row layout with icon, title, subtitle
- Updated `widget_today.xml` - Uses ListViews for scrollable sections
