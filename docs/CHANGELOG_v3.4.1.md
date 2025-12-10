# Changelog v3.4.1

**Release Date:** December 10, 2024

## 🐛 Bug Fixes

### Calculated Grades
- **Fixed calculated grades not appearing for all students**: Previously, calculated grade items only showed values for students who already had other grade records. Now all students in the class receive calculated grades immediately.

### Widget
- **Fixed widget not showing today's classes**: Day-of-week calculation was off by 1, causing wrong day's schedule to display.
- **Fixed widget schedule missing class names**: Schedule items now show class name along with time and session type.

### Tasks
- **Fixed tasks showing only one class**: Tasks with multiple classes now display all class names (comma-separated) in both home screen and widget.

## ✨ New Features

### Grade Entry
- **Sort by Score**: Added ability to sort students by their scores (ascending ↑ or descending ↓)
- **Sort State Display**: The sort button now shows the current sort mode (A-Z, Z-A, ID, Score ↑, Score ↓)

### Widget
- **Auto-refresh on app exit**: Widget automatically updates when you exit/minimize the app

## 🔧 Improvements

### UI
- Shortened sort option labels for cleaner display:
  - "ID/Matricule" → "ID"
  - "Score ↑ (Low to High)" → "Score ↑"
  - "Score ↓ (High to Low)" → "Score ↓"

### Widget Schedule Display
- Format: `ClassName - Time • SessionType (Room)`
- Example: `Math 101 - 08:00 • TD (Salle 1)`

### Task Display (Home & Widget)
- Now shows all associated classes from cross-reference table
- Fallback to legacy single classId for older tasks
