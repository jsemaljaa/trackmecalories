package com.jsemalja.trackmecalories

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class FoodRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getProductInfo(barcode: String): String {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        val response: String = client.get(url).body()

        val json = Json.parseToJsonElement(response).jsonObject
        val product = json["product"]?.jsonObject

        val name = product?.get("product_name")?.jsonPrimitive?.content ?: "Unknown Food"
        val nutrients = product?.get("nutriments")?.jsonObject
        val kcal = nutrients?.get("energy-kcal_100g")?.jsonPrimitive?.content ?: "0"

        return "$name ($kcal kcal/100g)"
    }
}