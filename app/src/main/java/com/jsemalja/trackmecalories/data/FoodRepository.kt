package com.jsemalja.trackmecalories.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Data class representing food nutritional information per 100g.
 */
data class FoodInfo(
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val barcode: String? = null,
    val unit: String = "g",
    // The amount kcalPer100 etc. are relative to: 100 for g/ml, 1 for "pcs".
    val referenceAmount: Double = 100.0
)

/**
 * Repository for fetching food data from external APIs (Open Food Facts).
 * Optionally backed by a [FoodDao] to cache barcode lookups and search results offline,
 * so repeat scans/searches don't always hit the network.
 */
class FoodRepository(private val dao: FoodDao? = null) {
    private val TAG = "FoodRepository"

    // Lightweight in-memory cache for repeated identical searches within this process's
    // lifetime. Persistent caching only applies to individual (barcode-keyed) products.
    private val searchCache = object : LinkedHashMap<String, List<FoodInfo>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FoodInfo>>) =
            size > 20
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Fetches product information from the Open Food Facts API v2, preferring a local
     * cache hit over the network.
     * @param barcode The UPC/EAN code of the product.
     * @return FoodInfo if found, null otherwise.
     */
    suspend fun getProductInfo(barcode: String): FoodInfo? {
        dao?.getCachedProduct(barcode)?.let {
            Log.d(TAG, "Cache hit for barcode: $barcode")
            return it.toFoodInfo()
        }
        Log.d(TAG, "Fetching product info for barcode: $barcode")
        return try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val response: String = client.get(url).body()

            val json = Json.parseToJsonElement(response).jsonObject
            val status = json["status"]?.jsonPrimitive?.intOrNull
            
            if (status != 1) {
                Log.w(TAG, "Product not found for barcode: $barcode, status: $status")
                return null
            }

            val product = json["product"]?.jsonObject ?: return null
            Log.d(TAG, "Successfully found product: ${product["product_name"]?.jsonPrimitive?.content}")

            val info = mapProductToFoodInfo(product, barcode)
            info.toCache()?.let { dao?.upsertCachedProduct(it) }
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product info: ${e.message}", e)
            null
        }
    }

    /**
     * Searches for products using the Open Food Facts Search API Version 2.
     * Optimized for English results and popularity to find generic/common items.
     * Ref: https://wiki.openfoodfacts.org/Open_Food_Facts_Search_API_Version_2
     */
    suspend fun searchProducts(query: String): List<FoodInfo> {
        val key = query.trim().lowercase()
        if (key.length < 2) return emptyList()
        searchCache[key]?.let {
            Log.d(TAG, "Serving search for '$query' from in-memory cache")
            return it
        }
        Log.d(TAG, "Searching for products with query: $query")
        return try {
            // Optimization: Refined query to prioritize common foods in English-speaking regions
            // Added cc=us,gb (country codes) and categories to help find standard items
            val url = "https://world.openfoodfacts.org/api/v2/search?" +
                    "search_terms=${query.trim()}" +
                    "&fields=code,product_name,product_name_en,generic_name_en,nutriments,nutrition_grades" +
                    "&page_size=50" +
                    "&sort_by=unique_scans_n" +
                    "&lc=en" +
                    "&cc=us" // Prioritize US results for more common generic English names

            Log.d(TAG, "Search URL V2 Optimized: $url")
            val response: String = client.get(url).body()
            val json = Json.parseToJsonElement(response).jsonObject
            val products = json["products"]?.jsonArray ?: return emptyList()

            Log.d(TAG, "Found ${products.size} products from API V2")

            val mappedResults = products.mapNotNull { 
                val productObj = it.jsonObject
                
                // Get name safely, prioritizing English
                val name = productObj["product_name_en"]?.jsonPrimitive?.content 
                    ?: productObj["product_name"]?.jsonPrimitive?.content 
                    ?: productObj["generic_name_en"]?.jsonPrimitive?.content
                    ?: "Unknown"

                // Skip items with essentially no nutritional info
                val nutrients = productObj["nutriments"]?.jsonObject
                val kcal = nutrients?.get("energy-kcal_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0
                
                // If it has a nutrition grade or kcal > 0, it's likely a valid entry
                val hasGrade = productObj["nutrition_grades"]?.jsonPrimitive?.contentOrNull != null
                
                if (kcal == 0.0 && !hasGrade && !query.lowercase().contains("water") && !query.lowercase().contains("spice")) {
                    Log.v(TAG, "Filtering out $name due to lack of nutrition data")
                    null
                } else {
                    mapProductToFoodInfo(productObj, productObj["code"]?.jsonPrimitive?.content)
                }
            }
            
            // Final quality check: ensure results that start with the query appear first
            val sorted = mappedResults.sortedByDescending {
                it.name.lowercase().startsWith(query.lowercase())
            }
            searchCache[key] = sorted
            sorted.forEach { info -> info.toCache()?.let { dao?.upsertCachedProduct(it) } }
            sorted
        } catch (e: Exception) {
            Log.e(TAG, "Error searching products: ${e.message}", e)
            emptyList()
        }
    }

    private fun mapProductToFoodInfo(product: JsonObject, barcode: String?): FoodInfo {
        // Prefer English names for a more consistent English search experience
        val name = product["product_name_en"]?.jsonPrimitive?.content 
            ?: product["product_name"]?.jsonPrimitive?.content 
            ?: product["generic_name_en"]?.jsonPrimitive?.content
            ?: "Unknown Food"

        val nutrients = product["nutriments"]?.jsonObject
        
        return FoodInfo(
            name = name,
            kcalPer100 = nutrients?.get("energy-kcal_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            proteinPer100 = nutrients?.get("protein_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            carbsPer100 = nutrients?.get("carbohydrates_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            fatPer100 = nutrients?.get("fat_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            fiberPer100 = nutrients?.get("fiber_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            saltPer100 = nutrients?.get("salt_100g")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            barcode = barcode
        )
    }
}
