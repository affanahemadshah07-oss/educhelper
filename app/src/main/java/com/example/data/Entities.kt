package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val instructor: String = "",
    val colorHex: String = "#1B3A5C",
    val attendedClasses: Int = 0,
    val totalClasses: Int = 0,
    val targetPercentage: Int = 75,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val subjectName: String,
    val room: String = "",
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val startMinutes: Int, // Minutes from midnight, e.g., 540 = 09:00
    val endMinutes: Int,   // Minutes from midnight, e.g., 600 = 10:00
    val colorHex: String = "#1B3A5C"
)

@Entity(tableName = "question_sets")
data class QuestionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String,
    val description: String = "",
    val colorHex: String = "#1B3A5C",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionSetId"])]
)
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionSetId: Long,
    val subjectName: String,
    val question: String,
    val answer: String,
    val distractorOne: String = "",
    val distractorTwo: String = "",
    val distractorThree: String = "",
    val hint: String = "",
    val masteryLevel: Int = 0, // 0 = New, 1 = Learning, 2 = Familiar, 3 = Mastered
    val timesReviewed: Int = 0,
    val timesCorrect: Int = 0,
    val lastReviewedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quiz_attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["questionSetId"])]
)
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionSetId: Long,
    val score: Int,
    val totalQuestions: Int,
    val durationSeconds: Int,
    val completedAt: Long = System.currentTimeMillis()
) {
    val percentage: Int
        get() = if (totalQuestions <= 0) 0 else (score * 100) / totalQuestions
}

data class QuestionSetWithDetails(
    @Embedded val questionSet: QuestionSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionSetId"
    )
    val cards: List<FlashcardEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionSetId"
    )
    val attempts: List<QuizAttemptEntity>
) {
    val masteredCount: Int
        get() = cards.count { it.masteryLevel >= 3 }

    val masteryFraction: Float
        get() = if (cards.isEmpty()) 0f else masteredCount.toFloat() / cards.size.toFloat()

    val bestScorePercent: Int?
        get() = attempts.maxOfOrNull { it.percentage }

    val latestAttempt: QuizAttemptEntity?
        get() = attempts.maxByOrNull { it.completedAt }
}

@Entity(tableName = "deadlines")
data class DeadlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectName: String,
    val type: String = "ASSIGNMENT", // EXAM, ASSIGNMENT, LAB, QUIZ
    val dueEpochMillis: Long,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 60,
    val createdAt: Long = System.currentTimeMillis()
)
