package com.genegebra.healthtracker.domain.model

import java.util.Date

data class HealthEntry(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val sessionId: String = "",
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val anxietyLevel: Int? = null,
    val createdAt: Date = Date()
) {
    val hasAnyValue: Boolean
        get() = systolic != null || diastolic != null || pulse != null || anxietyLevel != null

    val kerdoIndex: Double?
        get() {
            val d = diastolic ?: return null
            val p = pulse ?: return null
            if (p == 0) return null
            return (1.0 - d.toDouble() / p.toDouble()) * 100.0
        }

    val robinsonIndex: Double?
        get() {
            val s = systolic ?: return null
            val p = pulse ?: return null
            return s.toDouble() * p.toDouble() / 100.0
        }
}
