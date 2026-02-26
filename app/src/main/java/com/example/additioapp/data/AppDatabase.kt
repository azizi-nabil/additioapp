package com.example.additioapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.additioapp.data.dao.AttendanceDao
import com.example.additioapp.data.dao.AttendanceStatusDao
import com.example.additioapp.data.dao.BehaviorDao
import com.example.additioapp.data.dao.BehaviorTypeDao
import com.example.additioapp.data.dao.ClassDao
import com.example.additioapp.data.dao.EventDao
import com.example.additioapp.data.dao.GradeDao
import com.example.additioapp.data.dao.GradeCategoryDao
import com.example.additioapp.data.dao.ScheduleItemDao
import com.example.additioapp.data.dao.SessionDao
import com.example.additioapp.data.dao.StudentDao
import com.example.additioapp.data.dao.TaskDao
import com.example.additioapp.data.dao.TeacherAbsenceDao
import com.example.additioapp.data.dao.UnitDao
import com.example.additioapp.data.dao.StudentNoteDao
import com.example.additioapp.data.dao.ClassNoteDao
import com.example.additioapp.data.model.AttendanceRecordEntity
import com.example.additioapp.data.model.AttendanceStatusEntity
import com.example.additioapp.data.model.BehaviorRecordEntity
import com.example.additioapp.data.model.BehaviorTypeEntity
import com.example.additioapp.data.model.ClassEntity
import com.example.additioapp.data.model.EventEntity
import com.example.additioapp.data.model.GradeCategoryEntity
import com.example.additioapp.data.model.GradeItemEntity
import com.example.additioapp.data.model.GradeRecordEntity
import com.example.additioapp.data.model.ScheduleItemEntity
import com.example.additioapp.data.model.SessionEntity
import com.example.additioapp.data.model.StudentEntity
import com.example.additioapp.data.model.TaskEntity
import com.example.additioapp.data.model.TaskClassCrossRef
import com.example.additioapp.data.model.EventClassCrossRef
import com.example.additioapp.data.model.ScheduleItemClassCrossRef
import com.example.additioapp.data.model.TeacherAbsenceEntity
import com.example.additioapp.data.model.UnitEntity
import com.example.additioapp.data.model.StudentNoteEntity
import com.example.additioapp.data.model.ClassNoteEntity
import com.example.additioapp.data.model.GradeItemGroupEntity
import com.example.additioapp.data.dao.GradeItemGroupDao

@Database(
    entities = [
        ClassEntity::class,
        StudentEntity::class,
        AttendanceRecordEntity::class,
        GradeItemEntity::class,
        GradeRecordEntity::class,
        BehaviorRecordEntity::class,
        AttendanceStatusEntity::class,
        BehaviorTypeEntity::class,
        GradeCategoryEntity::class,
        SessionEntity::class,
        UnitEntity::class,
        EventEntity::class,
        TaskEntity::class,
        TaskClassCrossRef::class,
        EventClassCrossRef::class,
        ScheduleItemEntity::class,
        ScheduleItemClassCrossRef::class,
        TeacherAbsenceEntity::class,
        StudentNoteEntity::class,
        ClassNoteEntity::class,
        GradeItemGroupEntity::class
    ],
    version = 26,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun gradeDao(): GradeDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun attendanceStatusDao(): AttendanceStatusDao
    abstract fun behaviorTypeDao(): BehaviorTypeDao
    abstract fun gradeCategoryDao(): GradeCategoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun unitDao(): UnitDao
    abstract fun eventDao(): EventDao
    abstract fun taskDao(): TaskDao
    abstract fun scheduleItemDao(): ScheduleItemDao
    abstract fun teacherAbsenceDao(): TeacherAbsenceDao
    abstract fun studentNoteDao(): StudentNoteDao
    abstract fun classNoteDao(): ClassNoteDao
    abstract fun gradeItemGroupDao(): GradeItemGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from v6 to v7: Add events table and color to classes
        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add color column to classes table
                db.execSQL("ALTER TABLE classes ADD COLUMN color TEXT NOT NULL DEFAULT '#2196F3'")
                
                // Create events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        date INTEGER NOT NULL,
                        startTime TEXT,
                        endTime TEXT,
                        classId INTEGER,
                        eventType TEXT NOT NULL DEFAULT 'OTHER',
                        reminderMinutes INTEGER,
                        isAllDay INTEGER NOT NULL DEFAULT 0,
                        color TEXT,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_classId ON events(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_date ON events(date)")
            }
        }

        // Migration from v7 to v8: Add tasks table
        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        dueDate INTEGER,
                        classId INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_classId ON tasks(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDate ON tasks(dueDate)")
            }
        }

        // Migration from v8 to v9: Add schedule_items table
        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS schedule_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        classId INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        room TEXT NOT NULL DEFAULT '',
                        sessionType TEXT NOT NULL DEFAULT 'Cours',
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_items_classId ON schedule_items(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_items_dayOfWeek ON schedule_items(dayOfWeek)")
            }
        }

        // Migration from v9 to v10: Add performance indices
        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add indices to classes table for faster filtering
                db.execSQL("CREATE INDEX IF NOT EXISTS index_classes_year ON classes(year)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_classes_isArchived ON classes(isArchived)")
                // Add sessionId index to attendance_records for faster lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_records_sessionId ON attendance_records(sessionId)")
            }
        }

        // Migration from v10 to v11: Add extended student name fields
        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to students table
                db.execSQL("ALTER TABLE students ADD COLUMN matricule TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE students ADD COLUMN firstNameFr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE students ADD COLUMN lastNameFr TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE students ADD COLUMN firstNameAr TEXT")
                db.execSQL("ALTER TABLE students ADD COLUMN lastNameAr TEXT")
                // Add index on matricule for faster lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_students_matricule ON students(matricule)")
                
                // Migrate existing data: copy name to lastNameFr, studentId to matricule
                db.execSQL("UPDATE students SET lastNameFr = name, matricule = studentId WHERE name IS NOT NULL")
            }
        }

        // Migration from v11 to v12: Add notes column to students table
        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE students ADD COLUMN notes TEXT")
            }
        }

        // Migration from v12 to v13: Add task_class_cross_ref junction table
        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create the junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS task_class_cross_ref (
                        taskId INTEGER NOT NULL,
                        classId INTEGER NOT NULL,
                        PRIMARY KEY(taskId, classId),
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_class_cross_ref_taskId ON task_class_cross_ref(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_class_cross_ref_classId ON task_class_cross_ref(classId)")
                
                // Migrate existing classId data to junction table
                db.execSQL("""
                    INSERT INTO task_class_cross_ref (taskId, classId)
                    SELECT id, classId FROM tasks WHERE classId IS NOT NULL
                """.trimIndent())
            }
        }

        // Migration from v13 to v14: Add event_class_cross_ref junction table
        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create the junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS event_class_cross_ref (
                        eventId INTEGER NOT NULL,
                        classId INTEGER NOT NULL,
                        PRIMARY KEY(eventId, classId),
                        FOREIGN KEY(eventId) REFERENCES events(id) ON DELETE CASCADE,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_class_cross_ref_eventId ON event_class_cross_ref(eventId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_class_cross_ref_classId ON event_class_cross_ref(classId)")
                
                // Migrate existing classId data to junction table
                db.execSQL("""
                    INSERT INTO event_class_cross_ref (eventId, classId)
                    SELECT id, classId FROM events WHERE classId IS NOT NULL
                """.trimIndent())
            }
        }
        
        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add recurrence columns to events table
                db.execSQL("ALTER TABLE events ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE events ADD COLUMN recurrenceEndDate INTEGER")
                db.execSQL("ALTER TABLE events ADD COLUMN parentEventId INTEGER")
            }
        }
        
        private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create the junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS schedule_item_class_cross_ref (
                        scheduleItemId INTEGER NOT NULL,
                        classId INTEGER NOT NULL,
                        PRIMARY KEY(scheduleItemId, classId),
                        FOREIGN KEY(scheduleItemId) REFERENCES schedule_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_item_class_cross_ref_scheduleItemId ON schedule_item_class_cross_ref(scheduleItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_item_class_cross_ref_classId ON schedule_item_class_cross_ref(classId)")
                
                // Migrate existing classId data to junction table
                db.execSQL("""
                    INSERT INTO schedule_item_class_cross_ref (scheduleItemId, classId)
                    SELECT id, classId FROM schedule_items WHERE classId IS NOT NULL
                """.trimIndent())
            }
        }
        
        // Migration from v16 to v17: Add performance indices for foreign keys
        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add indices for frequently queried foreign keys to improve query performance
                db.execSQL("CREATE INDEX IF NOT EXISTS index_students_classId ON students(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_records_studentId ON attendance_records(studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grade_records_studentId ON grade_records(studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grade_records_gradeItemId ON grade_records(gradeItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_behavior_records_studentId ON behavior_records(studentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_behavior_records_classId ON behavior_records(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grade_items_classId ON grade_items(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_classId ON sessions(classId)")
            }
        }

        // Migration 17 to 18: Add teacher_absences table (old schema with classId)
        private val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS teacher_absences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        classId INTEGER NOT NULL,
                        sessionType TEXT NOT NULL,
                        absenceDate INTEGER NOT NULL,
                        reason TEXT,
                        replacementDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_absences_classId ON teacher_absences(classId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_absences_status ON teacher_absences(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_absences_absenceDate ON teacher_absences(absenceDate)")
            }
        }

        // Migration 18 to 19: Change classId to classIds (multi-class support)
        private val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create new table with classIds
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS teacher_absences_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        classIds TEXT NOT NULL,
                        sessionType TEXT NOT NULL,
                        absenceDate INTEGER NOT NULL,
                        reason TEXT,
                        replacementDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        notes TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                // Copy data, converting classId to classIds string
                db.execSQL("INSERT INTO teacher_absences_new SELECT id, CAST(classId AS TEXT), sessionType, absenceDate, reason, replacementDate, status, notes, createdAt FROM teacher_absences")
                // Drop old table
                db.execSQL("DROP TABLE teacher_absences")
                // Rename new table
                db.execSQL("ALTER TABLE teacher_absences_new RENAME TO teacher_absences")
                // Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_absences_status ON teacher_absences(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_absences_absenceDate ON teacher_absences(absenceDate)")
            }
        }

        // Migration 19 to 20: Add location to tasks and room to teacher_absences
        private val MIGRATION_19_20 = object : androidx.room.migration.Migration(19, 20) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT")
                db.execSQL("ALTER TABLE teacher_absences ADD COLUMN room TEXT")
            }
        }

        // Migration from v20 to v21: Add student_notes and class_notes tables
        private val MIGRATION_20_21 = object : androidx.room.migration.Migration(20, 21) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS student_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_student_notes_studentId ON student_notes(studentId)")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS class_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        classId INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        FOREIGN KEY(classId) REFERENCES classes(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_class_notes_classId ON class_notes(classId)")
            }
        }

        // Migration from v21 to v22: Add notes to sessions and grade_items
        private val MIGRATION_21_22 = object : androidx.room.migration.Migration(21, 22) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN notes TEXT")
                db.execSQL("ALTER TABLE grade_items ADD COLUMN notes TEXT")
            }
        }

        // Migration from v22 to v23: Add location to events
        private val MIGRATION_22_23 = object : androidx.room.migration.Migration(22, 23) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migration from v23 to v24: Add grade_item_groups table for student groups
        private val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS grade_item_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gradeItemId INTEGER NOT NULL,
                        groupNumber INTEGER NOT NULL DEFAULT 1,
                        studentId INTEGER NOT NULL,
                        FOREIGN KEY (gradeItemId) REFERENCES grade_items(id) ON DELETE CASCADE,
                        FOREIGN KEY (studentId) REFERENCES students(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grade_item_groups_gradeItemId ON grade_item_groups(gradeItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grade_item_groups_studentId ON grade_item_groups(studentId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grade_item_groups_gradeItemId_studentId ON grade_item_groups(gradeItemId, studentId)")
            }
        }

        // Migration from v24 to v25: Add groupId to students and grade_items for formula isolation
        private val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE students ADD COLUMN groupId INTEGER")
                db.execSQL("ALTER TABLE grade_items ADD COLUMN groupId INTEGER")
            }
        }

        // Migration from v25 to v26: Deduplicate grade_records and add unique index
        private val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Delete duplicates, keeping the one with the highest score (to avoid keeping a 0.0 that overwrote a real grade)
                // If scores are equal, keep the one with the higher ID
                db.execSQL("""
                    DELETE FROM grade_records 
                    WHERE id NOT IN (
                        SELECT id FROM (
                            SELECT id, ROW_NUMBER() OVER (
                                PARTITION BY studentId, gradeItemId 
                                ORDER BY score DESC, id DESC
                            ) as row_num 
                            FROM grade_records
                        ) WHERE row_num = 1
                    )
                """.trimIndent())
                
                // 2. Add the unique index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grade_records_studentId_gradeItemId ON grade_records(studentId, gradeItemId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "additio_database"
                )
                // Use proper migrations to preserve data
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26)
                // Only use destructive migration as last resort for older versions
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5)
                .build()
                .also { db -> seedDefaults(db) }
                INSTANCE = instance
                instance
            }
        }

        private fun seedDefaults(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                val defaults = listOf(
                    AttendanceStatusEntity(code = "P", label = "Present", countsAsPresent = true, colorHex = "#22C55E", orderIndex = 0),
                    AttendanceStatusEntity(code = "A", label = "Absent", countsAsPresent = false, colorHex = "#EF4444", orderIndex = 1),
                    AttendanceStatusEntity(code = "L", label = "Late", countsAsPresent = true, colorHex = "#EAB308", orderIndex = 2),
                    AttendanceStatusEntity(code = "E", label = "Excused", countsAsPresent = false, colorHex = "#6B7280", orderIndex = 3),
                    AttendanceStatusEntity(code = "JA", label = "Justified Absence", countsAsPresent = false, colorHex = "#6B7280", orderIndex = 4),
                    AttendanceStatusEntity(code = "JL", label = "Justified Late", countsAsPresent = true, colorHex = "#6B7280", orderIndex = 5),
                    AttendanceStatusEntity(code = "X", label = "Expelled", countsAsPresent = false, colorHex = "#1F2937", orderIndex = 6)
                )
                defaults.forEach { status -> db.attendanceStatusDao().insertStatus(status) }
            }
        }
    }
}
