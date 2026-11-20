package com.example

import com.example.data.FlashcardEntity
import com.example.data.QuestionSetEntity
import com.example.data.QuestionSetWithDetails
import com.example.data.QuizAttemptEntity
import com.example.data.TimetableSlotEntity
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealth
import com.example.domain.LiveClassState
import com.example.domain.QuizEngine
import com.example.domain.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyHubUnitTest {

    @Test
    fun attendanceCalculator_exactTarget_returnsZeroSafeSkipsAndBorderline() {
        val summary = AttendanceCalculator.calculate(attended = 15, total = 20, targetPercentage = 75)
        assertEquals(75.0, summary.percentage, 0.001)
        assertEquals(0, summary.safeSkips)
        assertEquals(0, summary.neededClasses)
        assertEquals(AttendanceHealth.BORDERLINE, summary.health)
    }

    @Test
    fun attendanceCalculator_aboveTarget_computesExactSafeSkips() {
        val summary = AttendanceCalculator.calculate(attended = 18, total = 20, targetPercentage = 75)
        assertEquals(4, summary.safeSkips)
        assertEquals(0, summary.neededClasses)
        assertEquals(AttendanceHealth.SAFE, summary.health)
    }

    @Test
    fun attendanceCalculator_belowTarget_computesExactNeededClasses() {
        val summary = AttendanceCalculator.calculate(attended = 10, total = 15, targetPercentage = 75)
        assertEquals(0, summary.safeSkips)
        assertEquals(5, summary.neededClasses)
        assertEquals(AttendanceHealth.CRITICAL, summary.health)
    }

    @Test
    fun attendanceCalculator_nonDivisibleTarget_roundsUpNeededClasses() {
        val summary = AttendanceCalculator.calculate(attended = 10, total = 14, targetPercentage = 76)
        assertEquals(3, summary.neededClasses)
        assertEquals(AttendanceHealth.CRITICAL, summary.health)
    }

    @Test
    fun liveClassState_detectsOngoingAndUpNext() {
        val slots = listOf(
            TimetableSlotEntity(id = 1, subjectName = "DSA", dayOfWeek = 1, startMinutes = 540, endMinutes = 600),
            TimetableSlotEntity(id = 2, subjectName = "OS", dayOfWeek = 1, startMinutes = 660, endMinutes = 720)
        )

        val ongoing = TimeUtils.computeLiveClassState(slots, nowMinutes = 570)
        assertTrue(ongoing is LiveClassState.Ongoing)
        assertEquals("DSA", (ongoing as LiveClassState.Ongoing).slot.subjectName)
        assertEquals(30, ongoing.minutesRemaining)

        val next = TimeUtils.computeLiveClassState(slots, nowMinutes = 615)
        assertTrue(next is LiveClassState.UpNext)
        assertEquals("OS", (next as LiveClassState.UpNext).slot.subjectName)
        assertEquals(45, next.minutesUntilStart)

        val done = TimeUtils.computeLiveClassState(slots, nowMinutes = 750)
        assertEquals(LiveClassState.CompletedForToday, done)
    }

    @Test
    fun quizEngine_prioritizesExplicitDistractorsAndBorrowsFromDeck() {
        val cardWithCustom = FlashcardEntity(
            id = 1,
            questionSetId = 10,
            subjectName = "OS",
            question = "What is thrashing?",
            answer = "Excessive paging",
            distractorOne = "Disk defragmentation",
            distractorTwo = "CPU overheating",
            distractorThree = "Thread pooling"
        )
        val cardWithoutCustom = FlashcardEntity(
            id = 2,
            questionSetId = 10,
            subjectName = "OS",
            question = "What is a mutex?",
            answer = "Binary lock"
        )

        val questions = QuizEngine.buildQuestions(
            deckCards = listOf(cardWithCustom, cardWithoutCustom),
            shuffle = false
        )

        assertEquals(2, questions.size)
        val q1 = questions[0]
        assertEquals(4, q1.options.size)
        assertTrue(q1.options.containsAll(listOf("Excessive paging", "Disk defragmentation", "CPU overheating", "Thread pooling")))

        val q2 = questions[1]
        assertEquals(2, q2.options.size)
        assertTrue(q2.options.contains("Binary lock"))
        assertTrue(q2.options.contains("Excessive paging"))
        assertEquals("01:05", QuizEngine.formatElapsedSeconds(65))
    }

    @Test
    fun questionSetWithDetails_computesMasteryAndBestQuizScore() {
        val set = QuestionSetEntity(id = 1, title = "ACID", subjectName = "DBMS")
        val cards = listOf(
            FlashcardEntity(id = 1, questionSetId = 1, subjectName = "DBMS", question = "Q1", answer = "A1", masteryLevel = 3),
            FlashcardEntity(id = 2, questionSetId = 1, subjectName = "DBMS", question = "Q2", answer = "A2", masteryLevel = 1)
        )
        val attempts = listOf(
            QuizAttemptEntity(id = 1, questionSetId = 1, score = 1, totalQuestions = 2, durationSeconds = 30),
            QuizAttemptEntity(id = 2, questionSetId = 1, score = 2, totalQuestions = 2, durationSeconds = 22)
        )

        val details = QuestionSetWithDetails(questionSet = set, cards = cards, attempts = attempts)
        assertEquals(1, details.masteredCount)
        assertEquals(0.5f, details.masteryFraction, 0.001f)
        assertEquals(100, details.bestScorePercent)
    }
}
