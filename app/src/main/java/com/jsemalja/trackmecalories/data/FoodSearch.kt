package com.jsemalja.trackmecalories.data

import java.text.Normalizer

/** Where a search result came from; shown on the result so you know how far to trust it. */
enum class SearchSource(@androidx.annotation.StringRes val label: Int) {
    MENU(com.jsemalja.trackmecalories.R.string.source_menu),
    KALORICKE(com.jsemalja.trackmecalories.R.string.source_kaloricke),
    OPEN_FOOD_FACTS(com.jsemalja.trackmecalories.R.string.source_off)
}

/**
 * One row in the unified search results. Either [food] is ready to log, or (for a live
 * Kalorické tabulky hit whose serving size isn't known yet) [ktHit] must be resolved first.
 * Macros are null when they haven't been loaded yet.
 */
data class SearchHit(
    val key: String,
    val name: String,
    val brand: String?,
    val detail: String?,
    val kcal: Double,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val unit: String,
    val source: SearchSource,
    val score: Double,
    val food: CustomFood? = null,
    val ktHit: KtHit? = null,
    val isCzech: Boolean = false
)

/**
 * Accent-insensitive, word-prefix matching and relevance scoring shared by every search
 * source, so results from different databases can be ranked in one list.
 *
 * - "kureci" finds "Kuřecí", "don pacin" finds "Don Pačínko" (the last word may be half-typed)
 * - Czech word endings are tolerated: "prsa" also matches "prso", "jogurt" matches "jogurtu"
 * - every query word must match somewhere in the name or brand, so the fuzzy junk that
 *   online databases return for half-typed words ("capric" -> "kuře na paprice") is dropped
 */
object SearchMatcher {

    fun tokens(query: String): List<String> {
        val all = query.normalizedForSearch().split(' ').filter { it.isNotEmpty() }
        // Single letters ("mcdonald s", "4 ks") match nearly everything; ignore them unless
        // they're all there is.
        return all.filter { it.length >= 2 }.ifEmpty { all }
    }

    /** Relevance of [name]/[brand] for [tokens], or null if it doesn't match all of them. */
    fun score(tokens: List<String>, name: String, brand: String? = null, extra: String? = null): Double? {
        if (tokens.isEmpty()) return null
        val normalizedName = name.normalizedForSearch()
        val nameWords = normalizedName.split(' ')
        val otherWords = listOfNotNull(brand, extra).joinToString(" ").normalizedForSearch().split(' ')

        var total = 0.0
        for (token in tokens) {
            val inName = wordScore(token, nameWords)
            val inOther = wordScore(token, otherWords) * 0.8
            val best = maxOf(inName, inOther)
            if (best <= 0.0) return null
            total += best
        }
        var score = total / tokens.size

        val phrase = tokens.joinToString(" ")
        score += when {
            normalizedName.startsWith(phrase) -> 1.0
            normalizedName.contains(phrase) -> 0.5
            else -> 0.0
        }
        // Prefer concise names: "Skyr" over "Skyr drink with strawberries and vanilla".
        score -= 0.05 * (nameWords.size - tokens.size).coerceAtLeast(0)
        return score
    }

    private fun wordScore(token: String, words: List<String>): Double {
        var best = 0.0
        for (word in words) {
            val s = when {
                word == token -> 3.0
                word.startsWith(token) -> 2.0
                token.length >= 4 && word.startsWith(token.dropLast(1)) -> 1.5
                token.length >= 5 && word.startsWith(token.dropLast(2)) -> 1.2
                token.length >= 4 && word.contains(token) -> 1.0 // compounds: "tvarohový" ~ "ovy"
                else -> 0.0
            }
            if (s > best) best = s
        }
        return best
    }

    /**
     * Query variants for online sources that can't prefix-match: the full query, plus the
     * query without its last (possibly half-typed) word. Results of both are re-filtered
     * locally against the full query, so "bageterie cae" finds "Caesar bageta".
     */
    fun onlineVariants(query: String): List<String> {
        val words = query.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val variants = mutableListOf(words.joinToString(" "))
        if (words.size >= 2 && words.dropLast(1).joinToString("").length >= 3) {
            variants += words.dropLast(1).joinToString(" ")
        }
        return variants
    }
}

/** Lowercase, accents stripped, punctuation collapsed to single spaces. */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase()
        .replace(NON_ALNUM, " ")
        .trim()

private val DIACRITICS = Regex("""\p{Mn}+""")
private val NON_ALNUM = Regex("""[^a-z0-9]+""")
