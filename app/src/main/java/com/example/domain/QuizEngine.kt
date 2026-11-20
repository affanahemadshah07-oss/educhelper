package com.example.domain

import com.example.data.FlashcardEntity

data class GeneratedQuizQuestion(
    val card: FlashcardEntity,
    val options: List<String>,
    val correctOption: String
)

object QuizEngine {

    /**
     * Generates up to [maxQuestions] multiple-choice questions for a self-test session.
     * Prioritizes each card's explicit distractors, then borrows distinct answers from
     * sibling cards in the same set or global pool so every question has up to 4 crisp choices.
     */
    fun buildQuestions(
        deckCards: List<FlashcardEntity>,
        fallbackPool: List<FlashcardEntity> = deckCards,
        maxQuestions: Int = 10,
        shuffle: Boolean = true
    ): List<GeneratedQuizQuestion> {
        if (deckCards.isEmpty()) return emptyList()

        val orderedDeck = if (shuffle) deckCards.shuffled() else deckCards
        val selected = orderedDeck.take(maxQuestions.coerceAtLeast(1))
        val poolAnswers = (deckCards + fallbackPool)
            .map { it.answer.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return selected.map { card ->
            val correct = card.answer.trim()
            val explicitDistractors = listOf(
                card.distractorOne.trim(),
                card.distractorTwo.trim(),
                card.distractorThree.trim()
            ).filter { it.isNotEmpty() && !it.equals(correct, ignoreCase = true) }
                .distinct()

            val neededFromPool = (3 - explicitDistractors.size).coerceAtLeast(0)
            val borrowedDistractors = if (neededFromPool > 0) {
                val candidates = poolAnswers.filter { ans ->
                    !ans.equals(correct, ignoreCase = true) &&
                        explicitDistractors.none { it.equals(ans, ignoreCase = true) }
                }
                (if (shuffle) candidates.shuffled() else candidates).take(neededFromPool)
            } else {
                emptyList()
            }

            val combined = (listOf(correct) + explicitDistractors + borrowedDistractors).toMutableList()
            if (combined.size < 2) {
                combined.add("Needs more review — answer not listed")
            }

            GeneratedQuizQuestion(
                card = card,
                options = if (shuffle) combined.shuffled() else combined,
                correctOption = correct
            )
        }
    }

    fun formatElapsedSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
