package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY createdAt ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY startMinutes ASC")
    fun getSlotsForDay(dayOfWeek: Int): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlotEntity): Long

    @Update
    suspend fun updateSlot(slot: TimetableSlotEntity)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteSlotById(id: Long)
}

@Dao
interface FlashcardDao {
    @Transaction
    @Query("SELECT * FROM question_sets ORDER BY createdAt DESC")
    fun getAllQuestionSetsWithDetails(): Flow<List<QuestionSetWithDetails>>

    @Query("SELECT * FROM question_sets ORDER BY createdAt DESC")
    fun getAllQuestionSets(): Flow<List<QuestionSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionSet(questionSet: QuestionSetEntity): Long

    @Update
    suspend fun updateQuestionSet(questionSet: QuestionSetEntity)

    @Query("DELETE FROM question_sets WHERE id = :id")
    suspend fun deleteQuestionSetById(id: Long)

    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE questionSetId = :questionSetId ORDER BY createdAt DESC")
    fun getFlashcardsBySet(questionSetId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity): Long

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity): Long

    @Query("SELECT * FROM quiz_attempts WHERE questionSetId = :questionSetId ORDER BY completedAt DESC LIMIT 10")
    fun getRecentAttemptsForSet(questionSetId: Long): Flow<List<QuizAttemptEntity>>
}

@Dao
interface DeadlineDao {
    @Query("SELECT * FROM deadlines ORDER BY isCompleted ASC, dueEpochMillis ASC")
    fun getAllDeadlines(): Flow<List<DeadlineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeadline(deadline: DeadlineEntity): Long

    @Update
    suspend fun updateDeadline(deadline: DeadlineEntity)

    @Query("DELETE FROM deadlines WHERE id = :id")
    suspend fun deleteDeadlineById(id: Long)
}
