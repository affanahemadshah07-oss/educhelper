package com.example.domain

import kotlin.math.roundToInt

enum class AttendanceHealth {
    EMPTY,
    SAFE,
    BORDERLINE,
    CRITICAL
}

data class AttendanceSummary(
    val percentage: Double,
    val formattedPercent: String,
    val health: AttendanceHealth,
    val safeSkips: Int,
    val neededClasses: Int,
    val statusHeadline: String,
    val statusDetail: String
)

object AttendanceCalculator {

    fun calculate(
        attended: Int,
        total: Int,
        targetPercentage: Int
    ): AttendanceSummary {
        val safeTarget = targetPercentage.coerceIn(1, 100)
        val safeTotal = total.coerceAtLeast(0)
        val safeAttended = attended.coerceIn(0, safeTotal)

        if (safeTotal == 0) {
            return AttendanceSummary(
                percentage = 0.0,
                formattedPercent = "--",
                health = AttendanceHealth.EMPTY,
                safeSkips = 0,
                neededClasses = 0,
                statusHeadline = "No classes logged",
                statusDetail = "Target: $safeTarget% • Mark your first class"
            )
        }

        val pct = (safeAttended.toDouble() / safeTotal.toDouble()) * 100.0
        val roundedPct = ((pct * 10.0).roundToInt() / 10.0)
        val formatted = if (roundedPct % 1.0 == 0.0) {
            "${roundedPct.toInt()}%"
        } else {
            String.format("%.1f%%", roundedPct)
        }

        val lhs = safeAttended * 100
        val rhs = safeTarget * safeTotal

        return if (lhs >= rhs) {
            val skips = (lhs - rhs) / safeTarget
            val health = if (skips >= 1) AttendanceHealth.SAFE else AttendanceHealth.BORDERLINE
            val headline = when {
                skips >= 2 -> "Safe to skip $skips classes"
                skips == 1 -> "Safe to skip 1 class"
                else -> "On target • Don't miss next"
            }
            val detail = if (skips > 0) {
                "Stays at or above $safeTarget% after skipping $skips"
            } else {
                "Skipping next drops below $safeTarget%"
            }
            AttendanceSummary(
                percentage = pct,
                formattedPercent = formatted,
                health = health,
                safeSkips = skips,
                neededClasses = 0,
                statusHeadline = headline,
                statusDetail = detail
            )
        } else {
            val needed = if (safeTarget >= 100) {
                -1
            } else {
                val numerator = rhs - lhs
                val denominator = 100 - safeTarget
                (numerator + denominator - 1) / denominator
            }
            val headline = when {
                needed < 0 -> "100% no longer reachable"
                needed == 1 -> "Attend next 1 class"
                else -> "Attend next $needed classes"
            }
            val detail = when {
                needed < 0 -> "Lower target below 100% to compute recovery"
                else -> "Needed consecutively to reach $safeTarget%"
            }
            AttendanceSummary(
                percentage = pct,
                formattedPercent = formatted,
                health = AttendanceHealth.CRITICAL,
                safeSkips = 0,
                neededClasses = needed.coerceAtLeast(0),
                statusHeadline = headline,
                statusDetail = detail
            )
        }
    }
}
