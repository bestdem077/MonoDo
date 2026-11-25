package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BirthdayEntity
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.NoteEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        NoteEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        CustomCategoryEntity::class,
        FocusSessionEntity::class,
        BirthdayEntity::class,
        RoutineTemplateEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun birthdayDao(): BirthdayDao
    abstract fun routineTemplateDao(): RoutineTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `taskId` INTEGER,
                        `taskTitle` TEXT NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `localEpochDay` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN reminderAt INTEGER")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `birthdays` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `birthYear` INTEGER,
                        `relationship` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `hoursBeforeMidnight` INTEGER NOT NULL,
                        `notifyAtMidnight` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routine_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `subtaskTitles` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monodo_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
