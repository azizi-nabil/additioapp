# Changelog v3.4.0

**Release Date:** December 9, 2024

## ✨ New Features

### Grades
- **Duplicate Grade Items**: Copy grade items to other classes via the menu (⋮ → Duplicate)
- **Formula `if()` Function**: Use conditional logic in calculated grades
  - Syntax: `if(condition, true_value, false_value)`
  - Returns `true_value` if condition > 0, otherwise `false_value`
- **Comparison Operators**: Use `>`, `<`, `>=`, `<=`, `==` in formulas
  - Example: `if(abs-td>3, 0, 20-abs-td*2)`
- **Behavior Variables**: Use positive and negative behavior counts in formulas
  - `pos` - Positive behavior count
  - `neg` - Negative behavior count
  - Example: `if(neg>0, Score-neg*2, Score+pos)`

### Settings
- **User Guide**: Comprehensive in-app documentation explaining all features
  - Located in Settings → Help & Tips → User Guide
  - Covers Classes, Students, Attendance, Grades, Behavior, Planner, Reports, Widgets, Backup, and Settings

## 🔧 Improvements

### Formula Help
- Updated to use HTML formatting for better readability
- Added documentation for `if()` function and comparison operators
- Added practical examples with attendance and behavior-based calculations

### Variables
- Renamed `pres-cours` to `pres-c` for consistency with other variable names

## 📋 Formula Reference

### Functions
- `avg(a, b, ...)` - Average
- `max(a, b, ...)` - Maximum  
- `min(a, b, ...)` - Minimum
- `if(cond, true, false)` - Conditional

### Operators
- Arithmetic: `+`, `-`, `*`, `/`, `(`, `)`
- Comparison: `>`, `<`, `>=`, `<=`, `==`

### Attendance Variables
- `abs-td` - TD absences count
- `abs-tp` - TP absences count
- `pres-c` - Course presences
- `tot-td` - Total TD sessions
- `tot-tp` - Total TP sessions
- `tot-c` - Total Course sessions

### Behavior Variables
- `pos` - Positive behavior count
- `neg` - Negative behavior count

### Example Formulas
```
avg(Test1, Test2, Exam)
CC * 0.4 + Exam * 0.6
if(abs-td>3, 0, 20-abs-td*2)
if(Score>=10, Score, 10)
pres-c / tot-c * 20
if(neg>0, Score-neg*2, Score+pos)
20 - neg*3 + pos*2
```
