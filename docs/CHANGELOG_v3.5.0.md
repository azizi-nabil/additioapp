# Changelog - Version 3.5.0

**Release Date:** December 18, 2024

---

## ✨ New Features

### 📝 Notes System
- **Student Notes**: Add, edit, and delete personal notes for each student
  - Access via student card menu → "Notes"
  - Each note is timestamped automatically
  - Notes displayed in chronological order

- **Class Notes**: Add, edit, and delete notes for each class
  - Access via long-press on class → "Notes"
  - Perfect for recording class-level observations

### 🎨 Modern UI/UX Improvements

#### Redesigned Action Menus
All action menus now use modern bottom sheets with icons:

- **Student Card Menu** (tap 3-dot icon):
  - ✏️ Edit - Modify student details
  - 📊 Marks - View grades report
  - 📋 Report - View absence report
  - 📝 Notes - Manage student notes
  - 🗑️ Delete - Remove student

- **Class Menu** (long-press class):
  - ✏️ Edit - Modify class details
  - 📄 Duplicate - Copy class with students
  - 📝 Notes - Manage class notes
  - 📦 Archive/Unarchive - Archive management
  - 🗑️ Delete - Remove class

- **Grade Item Menu** (tap 3-dot on grade item):
  - ✏️ Edit - Modify grade item
  - 📄 Duplicate - Copy to another class
  - 🗑️ Delete - Remove grade item

### 📱 Student Card Refactor
- Removed inline "Report" and "Marks" buttons from student cards
- All actions now consolidated in the 3-dot menu
- Cleaner, more compact card design

---

## 🔧 Technical Changes

### Database
- **Version**: 20 → 21
- Added `student_notes` table
- Added `class_notes` table
- Both tables cascade delete with parent

### New Files
- `StudentNoteEntity.kt` / `ClassNoteEntity.kt`
- `StudentNoteDao.kt` / `ClassNoteDao.kt`
- `StudentNotesDialog.kt` / `ClassNotesDialog.kt`
- `bottom_sheet_student_menu.xml`
- `bottom_sheet_class_menu.xml`
- `bottom_sheet_grade_item_menu.xml`
- New icons: `ic_notes`, `ic_edit`, `ic_archive`, `ic_unarchive`, `ic_content_copy`

---

## 📋 Bug Fixes
- None in this release

---

## 🔄 Migration Notes
- Database will auto-migrate from v20 to v21
- No manual steps required
