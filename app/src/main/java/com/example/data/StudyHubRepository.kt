package com.example.data

import com.example.domain.TimeUtils
import kotlinx.coroutines.flow.Flow

class StudyHubRepository(
    private val subjectDao: SubjectDao,
    private val timetableDao: TimetableDao,
    private val flashcardDao: FlashcardDao,
    private val deadlineDao: DeadlineDao
) {
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    val allTimetableSlots: Flow<List<TimetableSlotEntity>> = timetableDao.getAllSlots()
    val allQuestionSets: Flow<List<QuestionSetWithDetails>> = flashcardDao.getAllQuestionSetsWithDetails()
    val allFlashcards: Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    val allDeadlines: Flow<List<DeadlineEntity>> = deadlineDao.getAllDeadlines()

    // --- Subjects & Attendance ---
    suspend fun saveSubject(subject: SubjectEntity): Long {
        return if (subject.id == 0L) {
            subjectDao.insertSubject(subject)
        } else {
            subjectDao.updateSubject(subject)
            subject.id
        }
    }

    suspend fun deleteSubject(id: Long) = subjectDao.deleteSubjectById(id)

    suspend fun markAttendance(subject: SubjectEntity, present: Boolean): SubjectEntity {
        val updated = subject.copy(
            attendedClasses = subject.attendedClasses + if (present) 1 else 0,
            totalClasses = subject.totalClasses + 1
        )
        subjectDao.updateSubject(updated)
        return updated
    }

    suspend fun restoreSubject(subject: SubjectEntity) {
        subjectDao.updateSubject(subject)
    }

    // --- Timetable ---
    suspend fun saveTimetableSlot(slot: TimetableSlotEntity): Long {
        return if (slot.id == 0L) {
            timetableDao.insertSlot(slot)
        } else {
            timetableDao.updateSlot(slot)
            slot.id
        }
    }

    suspend fun deleteTimetableSlot(id: Long) = timetableDao.deleteSlotById(id)

    // --- Question Sets, Flashcards & Quiz Attempts ---
    suspend fun saveQuestionSet(questionSet: QuestionSetEntity): Long {
        return if (questionSet.id == 0L) {
            flashcardDao.insertQuestionSet(questionSet)
        } else {
            flashcardDao.updateQuestionSet(questionSet)
            questionSet.id
        }
    }

    suspend fun deleteQuestionSet(id: Long) = flashcardDao.deleteQuestionSetById(id)

    suspend fun saveFlashcard(card: FlashcardEntity): Long {
        return if (card.id == 0L) {
            flashcardDao.insertFlashcard(card)
        } else {
            flashcardDao.updateFlashcard(card)
            card.id
        }
    }

    suspend fun deleteFlashcard(id: Long) = flashcardDao.deleteFlashcardById(id)

    suspend fun updateFlashcardMastery(card: FlashcardEntity, knewIt: Boolean) {
        val nextLevel = if (knewIt) {
            (card.masteryLevel + 1).coerceAtMost(3)
        } else {
            (card.masteryLevel - 1).coerceAtLeast(0)
        }
        flashcardDao.updateFlashcard(
            card.copy(
                masteryLevel = nextLevel,
                timesReviewed = card.timesReviewed + 1,
                timesCorrect = card.timesCorrect + if (knewIt) 1 else 0,
                lastReviewedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordQuizAttempt(attempt: QuizAttemptEntity): Long {
        return flashcardDao.insertQuizAttempt(attempt)
    }

    // --- Deadlines ---
    suspend fun saveDeadline(deadline: DeadlineEntity): Long {
        return if (deadline.id == 0L) {
            deadlineDao.insertDeadline(deadline)
        } else {
            deadlineDao.updateDeadline(deadline)
            deadline.id
        }
    }

    suspend fun toggleDeadlineComplete(deadline: DeadlineEntity): DeadlineEntity {
        val updated = deadline.copy(isCompleted = !deadline.isCompleted)
        deadlineDao.updateDeadline(updated)
        return updated
    }

    suspend fun deleteDeadline(id: Long) = deadlineDao.deleteDeadlineById(id)

    /**
     * Explicitly triggered ONLY when the user taps "Load Sample Semester" on an empty workspace.
     */
    suspend fun seedSampleSemester() {
        val now = System.currentTimeMillis()
        val todayDow = TimeUtils.currentDayOfWeek(now)
        val nowMins = TimeUtils.currentMinutesFromMidnight(now)
        val nextDayDow = if (todayDow == 7) 1 else todayDow + 1

        val dsaId = subjectDao.insertSubject(
            SubjectEntity(
                name = "Data Structures & Algorithms",
                code = "CS204",
                instructor = "Dr. A. Mehta",
                colorHex = "#1B3A5C",
                attendedClasses = 19,
                totalClasses = 22,
                targetPercentage = 75
            )
        )
        val osId = subjectDao.insertSubject(
            SubjectEntity(
                name = "Operating Systems",
                code = "CS301",
                instructor = "Prof. R. Kapoor",
                colorHex = "#0E9F6E",
                attendedClasses = 12,
                totalClasses = 17,
                targetPercentage = 75
            )
        )
        val calcId = subjectDao.insertSubject(
            SubjectEntity(
                name = "Discrete Mathematics",
                code = "MA202",
                instructor = "Dr. S. Lin",
                colorHex = "#D97706",
                attendedClasses = 15,
                totalClasses = 20,
                targetPercentage = 75
            )
        )
        val dbId = subjectDao.insertSubject(
            SubjectEntity(
                name = "Database Systems",
                code = "CS305",
                instructor = "Prof. M. Patel",
                colorHex = "#7C3AED",
                attendedClasses = 17,
                totalClasses = 20,
                targetPercentage = 80
            )
        )

        val firstStart = (nowMins - 15).coerceIn(8 * 60, 19 * 60)
        val firstEnd = (firstStart + 60).coerceAtMost(23 * 60)
        val secondStart = (firstEnd + 30).coerceAtMost(21 * 60)
        val secondEnd = (secondStart + 60).coerceAtMost(23 * 60)
        val thirdStart = (secondEnd + 45).coerceAtMost(22 * 60)
        val thirdEnd = (thirdStart + 55).coerceAtMost(23 * 60 + 30)

        timetableDao.insertSlot(
            TimetableSlotEntity(
                subjectId = dsaId,
                subjectName = "Data Structures & Algorithms",
                room = "Hall B-204",
                dayOfWeek = todayDow,
                startMinutes = firstStart,
                endMinutes = firstEnd,
                colorHex = "#1B3A5C"
            )
        )
        timetableDao.insertSlot(
            TimetableSlotEntity(
                subjectId = osId,
                subjectName = "Operating Systems",
                room = "Lab CS-108",
                dayOfWeek = todayDow,
                startMinutes = secondStart,
                endMinutes = secondEnd,
                colorHex = "#0E9F6E"
            )
        )
        timetableDao.insertSlot(
            TimetableSlotEntity(
                subjectId = calcId,
                subjectName = "Discrete Mathematics",
                room = "Room A-312",
                dayOfWeek = todayDow,
                startMinutes = thirdStart,
                endMinutes = thirdEnd,
                colorHex = "#D97706"
            )
        )
        timetableDao.insertSlot(
            TimetableSlotEntity(
                subjectId = dbId,
                subjectName = "Database Systems",
                room = "Auditorium 2",
                dayOfWeek = nextDayDow,
                startMinutes = 10 * 60,
                endMinutes = 11 * 60,
                colorHex = "#7C3AED"
            )
        )

        // Seed structured Question Sets with rich MCQ distractors and initial quiz history
        val dsaSetId = flashcardDao.insertQuestionSet(
            QuestionSetEntity(
                title = "Trees, Hashing & Complexity",
                subjectName = "Data Structures & Algorithms",
                description = "Core asymptotic bounds, balanced trees, and hash collision strategies.",
                colorHex = "#1B3A5C"
            )
        )
        val osSetId = flashcardDao.insertQuestionSet(
            QuestionSetEntity(
                title = "Midterm Prep: Deadlocks & Memory",
                subjectName = "Operating Systems",
                description = "Coffman conditions, paging, thrashing, and CPU scheduling policies.",
                colorHex = "#0E9F6E"
            )
        )
        val dbSetId = flashcardDao.insertQuestionSet(
            QuestionSetEntity(
                title = "ACID & Normal Forms",
                subjectName = "Database Systems",
                description = "Relational transactions, isolation levels, and 3NF/BCNF decomposition.",
                colorHex = "#7C3AED"
            )
        )

        val sampleCards = listOf(
            FlashcardEntity(
                questionSetId = dsaSetId,
                subjectName = "Data Structures & Algorithms",
                question = "What is the worst-case time complexity of searching in a balanced Binary Search Tree (AVL / Red-Black)?",
                answer = "O(log n), because tree height is strictly bounded by rotations.",
                distractorOne = "O(n), because unbalanced chains can still form during lookup.",
                distractorTwo = "O(1) amortized using direct pointer indexing.",
                distractorThree = "O(n log n) due to recursive rebalancing on reads.",
                hint = "Think about height balancing after every insertion.",
                masteryLevel = 2,
                timesReviewed = 3,
                timesCorrect = 2
            ),
            FlashcardEntity(
                questionSetId = dsaSetId,
                subjectName = "Data Structures & Algorithms",
                question = "Which data structure guarantees O(1) amortized lookup by mapping keys to bucket indices?",
                answer = "Hash Table (HashMap) with chaining or open addressing.",
                distractorOne = "Min-Heap priority queue.",
                distractorTwo = "Doubly Linked List with sentinel tail.",
                distractorThree = "Adjacency Matrix graph representation.",
                hint = "Key-value mapping with a hash function.",
                masteryLevel = 3,
                timesReviewed = 4,
                timesCorrect = 4
            ),
            FlashcardEntity(
                questionSetId = dsaSetId,
                subjectName = "Data Structures & Algorithms",
                question = "Which graph algorithm finds shortest paths from a single source when all edge weights are non-negative?",
                answer = "Dijkstra's Algorithm using a min-priority queue.",
                distractorOne = "Kruskal's Minimum Spanning Tree Algorithm.",
                distractorTwo = "Depth-First Search (DFS) topological traversal.",
                distractorThree = "Tarjan's Strongly Connected Components Algorithm.",
                hint = "Greedily relaxes the nearest unvisited vertex.",
                masteryLevel = 1,
                timesReviewed = 1,
                timesCorrect = 1
            ),
            FlashcardEntity(
                questionSetId = osSetId,
                subjectName = "Operating Systems",
                question = "Name the four Coffman conditions required simultaneously for a deadlock to occur.",
                answer = "Mutual Exclusion, Hold and Wait, No Preemption, and Circular Wait.",
                distractorOne = "Starvation, Context Switching, Spooling, and Paging.",
                distractorTwo = "Atomicity, Consistency, Isolation, and Durability.",
                distractorThree = "Preemption, Aging, Segmentation, and Thrashing.",
                hint = "Four necessary conditions for resource lockups.",
                masteryLevel = 1,
                timesReviewed = 2,
                timesCorrect = 1
            ),
            FlashcardEntity(
                questionSetId = osSetId,
                subjectName = "Operating Systems",
                question = "What is thrashing in virtual memory systems?",
                answer = "Excessive paging where the CPU spends more time swapping pages than executing instructions.",
                distractorOne = "Corrupting kernel stack frames during an interrupt.",
                distractorTwo = "Fragmenting contiguous disk blocks into smaller sectors.",
                distractorThree = "Overheating CPU cores from continuous spinlocks.",
                hint = "Occurs when active working sets exceed physical RAM frames.",
                masteryLevel = 0
            ),
            FlashcardEntity(
                questionSetId = osSetId,
                subjectName = "Operating Systems",
                question = "Which CPU scheduling algorithm minimizes average waiting time for a known batch of burst lengths?",
                answer = "Shortest Job First (SJF) / Shortest Remaining Time First.",
                distractorOne = "First-Come, First-Served (FCFS).",
                distractorTwo = "Round Robin with a large time quantum.",
                distractorThree = "Static Priority without aging.",
                hint = "Run quick processes before long ones.",
                masteryLevel = 2,
                timesReviewed = 2,
                timesCorrect = 2
            ),
            FlashcardEntity(
                questionSetId = dbSetId,
                subjectName = "Database Systems",
                question = "What do the four letters in ACID transactions stand for?",
                answer = "Atomicity, Consistency, Isolation, and Durability.",
                distractorOne = "Availability, Concurrency, Indexing, and Distribution.",
                distractorTwo = "Aggregation, Constraints, Integrity, and Dependency.",
                distractorThree = "Authentication, Caching, Isolation, and Deduplication.",
                hint = "Core guarantees of reliable relational transactions.",
                masteryLevel = 3,
                timesReviewed = 3,
                timesCorrect = 3
            ),
            FlashcardEntity(
                questionSetId = dbSetId,
                subjectName = "Database Systems",
                question = "A relation is in Boyce-Codd Normal Form (BCNF) if and only if for every non-trivial functional dependency X -> Y:",
                answer = "X is a superkey of the relation.",
                distractorOne = "Y is part of the primary key.",
                distractorTwo = "All attributes are atomic with no repeating groups.",
                distractorThree = "There are no foreign keys referencing another table.",
                hint = "Stricter than 3NF regarding determinants.",
                masteryLevel = 1
            )
        )
        sampleCards.forEach { flashcardDao.insertFlashcard(it) }

        flashcardDao.insertQuizAttempt(
            QuizAttemptEntity(
                questionSetId = dsaSetId,
                score = 3,
                totalQuestions = 3,
                durationSeconds = 42,
                completedAt = now - 6 * 3_600_000L
            )
        )
        flashcardDao.insertQuizAttempt(
            QuizAttemptEntity(
                questionSetId = osSetId,
                score = 2,
                totalQuestions = 3,
                durationSeconds = 58,
                completedAt = now - 3 * 3_600_000L
            )
        )

        val hourMs = 3_600_000L
        val dayMs = 24 * hourMs
        deadlineDao.insertDeadline(
            DeadlineEntity(
                title = "Operating Systems Midterm Exam",
                subjectName = "Operating Systems",
                type = "EXAM",
                dueEpochMillis = now + 2 * dayMs + 3 * hourMs,
                notes = "Chapters 1–5: Processes, Threads, CPU Scheduling, Deadlocks",
                reminderEnabled = true,
                reminderMinutesBefore = 120
            )
        )
        deadlineDao.insertDeadline(
            DeadlineEntity(
                title = "Graph Shortest-Path Lab Report",
                subjectName = "Data Structures & Algorithms",
                type = "LAB",
                dueEpochMillis = now + 18 * hourMs,
                notes = "Submit Dijkstra & Bellman-Ford benchmark plots",
                reminderEnabled = true,
                reminderMinutesBefore = 60
            )
        )
        deadlineDao.insertDeadline(
            DeadlineEntity(
                title = "SQL Normalization Problem Set #4",
                subjectName = "Database Systems",
                type = "ASSIGNMENT",
                dueEpochMillis = now + 4 * dayMs,
                notes = "3NF & BCNF decomposition proofs",
                reminderEnabled = false,
                reminderMinutesBefore = 60
            )
        )
    }
}
