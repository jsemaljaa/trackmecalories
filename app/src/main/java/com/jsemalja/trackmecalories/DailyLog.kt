package com.jsemalja.trackmecalories
import java.time.LocalDate

data class FoodLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val salt: Double,
    val date: LocalDate = LocalDate.now()
)