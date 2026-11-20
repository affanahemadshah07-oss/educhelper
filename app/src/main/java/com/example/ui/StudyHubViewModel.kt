package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeadlineEntity
import com.example.data.FlashcardEntity
import com.example.data.QuestionSetEntity
import com.example.data.QuestionSetWithDetails
import com.example.data.QuizAttemptEntity
import com.example.data.StudyHubDatabase
import com.example.data.StudyHubRepository
import com.example.data.SubjectEntity
import com.example.data.TimetableSlotEntity
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceSummary
import com.example.domain.GeneratedQuizQuestion
import com.example.domain.LiveClassState
import com.example.domain.QuizEngine
import com.example.domain.TimeUtils
import com.example.notifications.DeadlineReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
    TODAY,
    ATTENDANCE,
    FLASHCARDS,
    DEADLINES
}

data class QuizSessionState(
    val questionSetId: Long?, // Non-null when testing a specific QuestionSet so attempts persist to Room
    val title: String,
    val questions: List<GeneratedQuizQuestion>,
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val correctCount: Int = 0,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val elapsedSeconds: Int = 0,
    val isFinished: Boolean = false
)

data class FlashcardStudyState(
    val questionSetId: Long?,
    val title: String,
    val cards: List<FlashcardEntity>,
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val knownCount: Int = 0,
    val reviewAgainCount: Int = 0,
    val isCompleted: Boolean = false
)

data class AttendanceUndoSnapshot(
    val previousSubject: SubjectEntity,
    val actionLabel: String
)

data class StudyHubUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val allSlots: List<TimetableSlotEntity> = emptyList(),
    val questionSets: List<QuestionSetWithDetails> = emptyList(),
    val flashcards: List<FlashcardEntity> = emptyList(),
    val deadlines: List<DeadlineEntity> = emptyList(),
    val selectedDayOfWeek: Int = TimeUtils.currentDayOfWeek(),
    val todayDayOfWeek: Int = TimeUtils.currentDayOfWeek(),
    val nowMillis: Long = System.currentTimeMillis(),
    val overallAttendance: AttendanceSummary = AttendanceCalculator.calculate(0, 0, 75),
    val defaultTargetPercent: Int = 75,
    val liveClassState: LiveClassState = LiveClassState.NoClassesToday,
    val selectedFlashcardSubject: String = "All",
    val activeQuestionSetId: Long? = null,
    val selectedDeadlineFilter: String = "ALL" // ALL, EXAM, ASSIGNMENT, COMPLETED
) {
    val selectedDaySlots: List<TimetableSlotEntity>
        get() = allSlots.filter { it.dayOfWeek == selectedDayOfWeek }.sortedBy { it.startMinutes }

    val pendingDeadlinesCount: Int
        get() = deadlines.count { !it.isCompleted }

    val nextPendingDeadline: DeadlineEntity?
        get() = deadlines.firstOrNull { !it.isCompleted }

    val isWorkspaceEmpty: Boolean
        get() = subjects.isEmpty() && allSlots.isEmpty() && questionSets.isEmpty() && deadlines.isEmpty()

    val flashcardSubjects: List<String>
        get() = listOf("All") + questionSets.map { it.questionSet.subjectName }.distinct().sorted()

    val filteredQuestionSets: List<QuestionSetWithDetails>
        get() = if (selectedFlashcardSubject == "All") {
            questionSets
        } else {
            questionSets.filter { it.questionSet.subjectName == selectedFlashcardSubject }
        }

    val activeQuestionSet: QuestionSetWithDetails?
        get() = activeQuestionSetId?.let { id -> questionSets.firstOrNull { it.questionSet.id == id } }

    val filteredFlashcards: List<FlashcardEntity>
        get() = if (selectedFlashcardSubject == "All") {
            flashcards
        } else {
            flashcards.filter { it.subjectName == selectedFlashcardSubject }
        }

    val filteredDeadlines: List<DeadlineEntity>
        get() = when (selectedDeadlineFilter) {
            "EXAM" -> deadlines.filter { !it.isCompleted && (it.type == "EXAM" || it.type == "QUIZ") }
            "ASSIGNMENT" -> deadlines.filter { !it.isCompleted && (it.type == "ASSIGNMENT" || it.type == "LAB") }
            "COMPLETED" -> deadlines.filter { it.isCompleted }
            else -> deadlines.filter { !it.isCompleted }
        }
}

class StudyHubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudyHubDatabase.getInstance(application)
    private val repository = StudyHubRepository(
        subjectDao = database.subjectDao(),
        timetableDao = database.timetableDao(),
        flashcardDao = database.flashcardDao(),
        deadlineDao = database.deadlineDao()
    )
    private val reminderScheduler = DeadlineReminderScheduler(application)

    private val _selectedDayOfWeek = MutableStateFlow(TimeUtils.currentDayOfWeek())
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val _defaultTargetPercent = MutableStateFlow(75)
    private val _selectedFlashcardSubject = MutableStateFlow("All")
    private val _activeQuestionSetId = MutableStateFlow<Long?>(null)
    private val _selectedDeadlineFilter = MutableStateFlow("ALL")

    private val _activeTab = MutableStateFlow(MainTab.TODAY)
    val activeTab: StateFlow<MainTab> = _activeTab.asStateFlow()

    private val _studySession = MutableStateFlow<FlashcardStudyState?>(null)
    val studySession: StateFlow<FlashcardStudyState?> = _studySession.asStateFlow()

    private val _quizSession = MutableStateFlow<QuizSessionState?>(null)
    val quizSession: StateFlow<QuizSessionState?> = _quizSession.asStateFlow()

    private val _lastAttendanceUndo = MutableStateFlow<AttendanceUndoSnapshot?>(null)
    val lastAttendanceUndo: StateFlow<AttendanceUndoSnapshot?> = _lastAttendanceUndo.asStateFlow()

    private data class FilterState(
        val selectedDay: Int,
        val nowMillis: Long,
        val defaultTarget: Int,
        val flashcardSubject: String,
        val activeSetId: Long?,
        val deadlineFilter: String
    )

    private data class FlashcardDataBundle(
        val questionSets: List<QuestionSetWithDetails>,
        val flashcards: List<FlashcardEntity>
    )

    private val scheduleFilterFlow = combine(
        _selectedDayOfWeek,
        _nowMillis,
        _defaultTargetPercent
    ) { day, now, target ->
        Triple(day, now, target)
    }

    private val studyFilterFlow = combine(
        _selectedFlashcardSubject,
        _activeQuestionSetId,
        _selectedDeadlineFilter
    ) { flashcardSubject, activeSetId, deadlineFilter ->
        Triple(flashcardSubject, activeSetId, deadlineFilter)
    }

    private val filterFlow = combine(
        scheduleFilterFlow,
        studyFilterFlow
    ) { (day, now, target), (flashcardSubject, activeSetId, deadlineFilter) ->
        FilterState(
            selectedDay = day,
            nowMillis = now,
            defaultTarget = target,
            flashcardSubject = flashcardSubject,
            activeSetId = activeSetId,
            deadlineFilter = deadlineFilter
        )
    }

    private val flashcardBundleFlow = combine(
        repository.allQuestionSets,
        repository.allFlashcards
    ) { sets, cards ->
        FlashcardDataBundle(sets, cards)
    }

    val uiState: StateFlow<StudyHubUiState> = combine(
        repository.allSubjects,
        repository.allTimetableSlots,
        flashcardBundleFlow,
        repository.allDeadlines,
        filterFlow
    ) { subjects, slots, fcBundle, deadlines, filters ->
        val totalAttended = subjects.sumOf { it.attendedClasses }
        val totalConducted = subjects.sumOf { it.totalClasses }
        val overall = AttendanceCalculator.calculate(
            attended = totalAttended,
            total = totalConducted,
            targetPercentage = filters.defaultTarget
        )
        val todayDow = TimeUtils.currentDayOfWeek(filters.nowMillis)
        val todaySlots = slots.filter { it.dayOfWeek == todayDow }.sortedBy { it.startMinutes }
        val liveState = TimeUtils.computeLiveClassState(
            todaySlots = todaySlots,
            nowMinutes = TimeUtils.currentMinutesFromMidnight(filters.nowMillis)
        )

        StudyHubUiState(
            subjects = subjects,
            allSlots = slots,
            questionSets = fcBundle.questionSets,
            flashcards = fcBundle.flashcards,
            deadlines = deadlines,
            selectedDayOfWeek = filters.selectedDay,
            todayDayOfWeek = todayDow,
            nowMillis = filters.nowMillis,
            overallAttendance = overall,
            defaultTargetPercent = filters.defaultTarget,
            liveClassState = liveState,
            selectedFlashcardSubject = filters.flashcardSubject,
            activeQuestionSetId = filters.activeSetId,
            selectedDeadlineFilter = filters.deadlineFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudyHubUiState()
    )

    init {
        // Live clock ticker every 30s for timetable current/next class indicator
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                _nowMillis.value = System.currentTimeMillis()
            }
        }
        // Live 1-second timer while an active quiz session is running
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                _quizSession.update { session ->
                    if (session != null && !session.isFinished) {
                        val secs = ((System.currentTimeMillis() - session.startedAtMillis) / 1000L).toInt().coerceAtLeast(0)
                        session.copy(elapsedSeconds = secs)
                    } else {
                        session
                    }
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _activeTab.value = tab
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDayOfWeek.value = dayOfWeek.coerceIn(1, 7)
    }

    fun updateDefaultTargetPercent(target: Int) {
        _defaultTargetPercent.value = target.coerceIn(50, 95)
    }

    fun selectFlashcardSubject(subject: String) {
        _selectedFlashcardSubject.value = subject
    }

    fun openQuestionSet(setId: Long?) {
        _activeQuestionSetId.value = setId
    }

    fun selectDeadlineFilter(filter: String) {
        _selectedDeadlineFilter.value = filter
    }

    // --- Subjects & Attendance ---
    fun saveSubject(subject: SubjectEntity) {
        viewModelScope.launch { repository.saveSubject(subject) }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch { repository.deleteSubject(subjectId) }
    }

    fun markAttendance(subject: SubjectEntity, present: Boolean) {
        viewModelScope.launch {
            _lastAttendanceUndo.value = AttendanceUndoSnapshot(
                previousSubject = subject,
                actionLabel = if (present) "Marked Present for ${subject.name}" else "Marked Absent for ${subject.name}"
            )
            repository.markAttendance(subject, present)
        }
    }

    fun undoLastAttendance() {
        val snapshot = _lastAttendanceUndo.value ?: return
        _lastAttendanceUndo.value = null
        viewModelScope.launch { repository.restoreSubject(snapshot.previousSubject) }
    }

    fun clearUndoSnackbar() {
        _lastAttendanceUndo.value = null
    }

    // --- Timetable ---
    fun saveTimetableSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch { repository.saveTimetableSlot(slot) }
    }

    fun deleteTimetableSlot(slotId: Long) {
        viewModelScope.launch { repository.deleteTimetableSlot(slotId) }
    }

    // --- Question Sets, Flashcards & Self-Test Quizzes ---
    fun saveQuestionSet(questionSet: QuestionSetEntity) {
        viewModelScope.launch { repository.saveQuestionSet(questionSet) }
    }

    fun deleteQuestionSet(setId: Long) {
        viewModelScope.launch {
            if (_activeQuestionSetId.value == setId) {
                _activeQuestionSetId.value = null
            }
            repository.deleteQuestionSet(setId)
        }
    }

    fun saveFlashcard(card: FlashcardEntity) {
        viewModelScope.launch { repository.saveFlashcard(card) }
    }

    fun deleteFlashcard(cardId: Long) {
        viewModelScope.launch { repository.deleteFlashcard(cardId) }
    }

    fun startStudyForSet(setWithDetails: QuestionSetWithDetails) {
        val cards = setWithDetails.cards.sortedBy { it.masteryLevel }
        if (cards.isEmpty()) return
        _studySession.value = FlashcardStudyState(
            questionSetId = setWithDetails.questionSet.id,
            title = setWithDetails.questionSet.title,
            cards = cards
        )
    }

    fun startStudySession(subjectFilter: String) {
        val cards = uiState.value.flashcards.let { list ->
            if (subjectFilter == "All") list else list.filter { it.subjectName == subjectFilter }
        }.sortedBy { it.masteryLevel }
        if (cards.isEmpty()) return
        _studySession.value = FlashcardStudyState(
            questionSetId = null,
            title = if (subjectFilter == "All") "All Question Sets" else subjectFilter,
            cards = cards
        )
    }

    fun toggleStudyCardFlip() {
        _studySession.update { session -> session?.copy(isFlipped = !session.isFlipped) }
    }

    fun recordStudyCardResult(knewIt: Boolean) {
        val session = _studySession.value ?: return
        val currentCard = session.cards.getOrNull(session.currentIndex) ?: return

        viewModelScope.launch { repository.updateFlashcardMastery(currentCard, knewIt) }

        val nextIdx = session.currentIndex + 1
        _studySession.value = session.copy(
            currentIndex = nextIdx,
            isFlipped = false,
            knownCount = session.knownCount + if (knewIt) 1 else 0,
            reviewAgainCount = session.reviewAgainCount + if (knewIt) 0 else 1,
            isCompleted = nextIdx >= session.cards.size
        )
    }

    fun restartStudySession() {
        val session = _studySession.value ?: return
        val targetSet = session.questionSetId?.let { id ->
            uiState.value.questionSets.firstOrNull { it.questionSet.id == id }
        }
        if (targetSet != null) {
            startStudyForSet(targetSet)
        } else {
            startStudySession(uiState.value.selectedFlashcardSubject)
        }
    }

    fun closeStudySession() {
        _studySession.value = null
    }

    fun startQuizForSet(setWithDetails: QuestionSetWithDetails) {
        val questions = QuizEngine.buildQuestions(
            deckCards = setWithDetails.cards,
            fallbackPool = uiState.value.flashcards,
            maxQuestions = 10
        )
        if (questions.isEmpty()) return
        _quizSession.value = QuizSessionState(
            questionSetId = setWithDetails.questionSet.id,
            title = setWithDetails.questionSet.title,
            questions = questions,
            startedAtMillis = System.currentTimeMillis()
        )
    }

    fun startQuickSelfTest(subjectFilter: String) {
        val allCards = uiState.value.flashcards
        val deck = if (subjectFilter == "All") allCards else allCards.filter { it.subjectName == subjectFilter }
        val questions = QuizEngine.buildQuestions(
            deckCards = deck,
            fallbackPool = allCards,
            maxQuestions = 10
        )
        if (questions.isEmpty()) return
        _quizSession.value = QuizSessionState(
            questionSetId = null,
            title = if (subjectFilter == "All") "Mixed Self-Test" else "$subjectFilter Quiz",
            questions = questions,
            startedAtMillis = System.currentTimeMillis()
        )
    }

    fun submitQuizOption(option: String) {
        val session = _quizSession.value ?: return
        if (session.selectedOption != null) return
        val current = session.questions.getOrNull(session.currentIndex) ?: return
        val isCorrect = option == current.correctOption

        viewModelScope.launch { repository.updateFlashcardMastery(current.card, isCorrect) }

        _quizSession.value = session.copy(
            selectedOption = option,
            correctCount = session.correctCount + if (isCorrect) 1 else 0
        )
    }

    fun advanceQuizQuestion() {
        val session = _quizSession.value ?: return
        val nextIndex = session.currentIndex + 1
        val finished = nextIndex >= session.questions.size
        val finalDuration = ((System.currentTimeMillis() - session.startedAtMillis) / 1000L)
            .toInt()
            .coerceAtLeast(1)

        if (finished && session.questionSetId != null) {
            viewModelScope.launch {
                repository.recordQuizAttempt(
                    QuizAttemptEntity(
                        questionSetId = session.questionSetId,
                        score = session.correctCount,
                        totalQuestions = session.questions.size,
                        durationSeconds = finalDuration
                    )
                )
            }
        }

        _quizSession.value = session.copy(
            currentIndex = nextIndex,
            selectedOption = null,
            elapsedSeconds = if (finished) finalDuration else session.elapsedSeconds,
            isFinished = finished
        )
    }

    fun restartQuizSession() {
        val session = _quizSession.value ?: return
        val targetSet = session.questionSetId?.let { id ->
            uiState.value.questionSets.firstOrNull { it.questionSet.id == id }
        }
        if (targetSet != null) {
            startQuizForSet(targetSet)
        } else {
            startQuickSelfTest(uiState.value.selectedFlashcardSubject)
        }
    }

    fun closeQuizSession() {
        _quizSession.value = null
    }

    // --- Deadlines & Local Reminders ---
    fun saveDeadline(deadline: DeadlineEntity) {
        viewModelScope.launch {
            val savedId = repository.saveDeadline(deadline)
            reminderScheduler.schedule(deadline.copy(id = savedId))
        }
    }

    fun toggleDeadlineComplete(deadline: DeadlineEntity) {
        viewModelScope.launch {
            val updated = repository.toggleDeadlineComplete(deadline)
            if (updated.isCompleted) {
                reminderScheduler.cancel(updated.id)
            } else {
                reminderScheduler.schedule(updated)
            }
        }
    }

    fun deleteDeadline(deadlineId: Long) {
        viewModelScope.launch {
            reminderScheduler.cancel(deadlineId)
            repository.deleteDeadline(deadlineId)
        }
    }

    fun loadSampleSemester() {
        viewModelScope.launch { repository.seedSampleSemester() }
    }
}
