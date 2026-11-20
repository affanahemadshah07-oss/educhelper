package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubjectEntity::class,
        TimetableSlotEntity::class,
        QuestionSetEntity::class,
        FlashcardEntity::class,
        QuizAttemptEntity::class,
        DeadlineEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyHubDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun deadlineDao(): DeadlineDao

    companion object {
        @Volatile
        private var INSTANCE: StudyHubDatabase? = null

        fun getInstance(context: Context): StudyHubDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyHubDatabase::class.java,
                    "study_hub_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
