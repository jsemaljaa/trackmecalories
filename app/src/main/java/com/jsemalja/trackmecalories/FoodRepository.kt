package com.jsemalja.trackmecalories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

data class FoodInfo(
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val barcode: String? = null
)

class FoodRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getProductInfo(barcode: String): FoodInfo? {
        return try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val response: String = client.get(url).body()

            val json = Json.parseToJsonElement(response).jsonObject
            val status = json["status"]?.jsonPrimitive?.intOrNull
            
            if (status != 1) return null // Product not found

            val product = json["product"]?.jsonObject ?: return null

            val name = product["product_name"]?.jsonPrimitive?.content ?: "Unknown Food"
            val nutrients = product["nutriments"]?.jsonObject
            
            FoodInfo(
                name = name,
                kcalPer100 = nutrients?.get("energy-kcal_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                proteinPer100 = nutrients?.get("protein_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                carbsPer100 = nutrients?.get("carbohydrates_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                fatPer100 = nutrients?.get("fat_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                fiberPer100 = nutrients?.get("fiber_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                saltPer100 = nutrients?.get("salt_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                barcode = barcode
            )
        } catch (e: Exception) {
            null
        }
    }
}