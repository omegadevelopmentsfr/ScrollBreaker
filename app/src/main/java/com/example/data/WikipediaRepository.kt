package com.example.data

import com.example.data.models.WikiSearchResponse
import com.example.data.models.WikiSummaryResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class WikipediaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val summaryAdapter = moshi.adapter(WikiSummaryResponse::class.java)
    private val searchAdapter = moshi.adapter(WikiSearchResponse::class.java)

    /**
     * Fetches an article summary given a language, topic, and content source.
     */
    suspend fun fetchArticle(lang: String, topic: String?, source: String = "all"): Result<WikiSummaryResponse> = withContext(Dispatchers.IO) {
        val cleanLang = lang.lowercase().trim().ifEmpty { "en" }
        val cleanTopic = topic?.trim()

        val cleanSource = source.lowercase().trim()

        if (cleanSource == "devto" || (cleanSource == "all" && Random.nextInt(100) < 25)) {
            val devToResult = fetchDevToArticle()
            if (devToResult.isSuccess) {
                return@withContext devToResult
            }
        }

        val domain = resolveDomain(cleanSource)

        try {
            if (!cleanTopic.isNullOrEmpty() && !cleanTopic.equals("Random", ignoreCase = true) && !cleanTopic.equals("Alea", ignoreCase = true)) {
                // Try topic search first
                val searchResult = searchTopic(cleanLang, cleanTopic, domain)
                if (searchResult.isSuccess) {
                    val titles = searchResult.getOrNull()
                    if (!titles.isNullOrEmpty()) {
                        val randomTitle = titles.random(Random(System.currentTimeMillis()))
                        val summaryResult = fetchSummaryByTitle(cleanLang, randomTitle, domain)
                        if (summaryResult.isSuccess) {
                            return@withContext summaryResult
                        }
                    }
                }
            }

            // Fallback to random summary
            val randomResult = fetchRandomSummary(cleanLang, domain)
            if (randomResult.isSuccess) {
                return@withContext randomResult
            }

            // Ultimate fallback to built-in offline summary
            Result.success(getFallbackArticle(cleanLang, cleanTopic))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(getFallbackArticle(cleanLang, cleanTopic))
        }
    }

    private fun resolveDomain(source: String): String {
        return when (source.lowercase().trim()) {
            "wikinews" -> "wikinews.org"
            "wikiquote" -> "wikiquote.org"
            "wikivoyage" -> "wikivoyage.org"
            "wikibooks" -> "wikibooks.org"
            "wikipedia" -> "wikipedia.org"
            else -> listOf("wikipedia.org", "wikipedia.org", "wikinews.org", "wikiquote.org", "wikivoyage.org", "wikibooks.org").random()
        }
    }

    private fun fetchDevToArticle(): Result<WikiSummaryResponse> {
        return try {
            val url = "https://dev.to/api/articles?per_page=30&top=14"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ScrollBreak/1.0 (Android App)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val jsonArray = org.json.JSONArray(body)
                if (jsonArray.length() > 0) {
                    val randomIndex = Random(System.currentTimeMillis()).nextInt(jsonArray.length())
                    val item = jsonArray.getJSONObject(randomIndex)
                    val title = item.optString("title", "Article Tech & Culture")
                    val description = item.optString("description", "Dev.to Tech Article")
                    val articleUrl = item.optString("url", "https://dev.to")
                    val coverImage = item.optString("cover_image", "")

                    val summary = WikiSummaryResponse(
                        title = title,
                        description = "Dev.to Tech & Digital",
                        extract = description.ifEmpty { "Découvrez cet article de fond sur la technologie et le numérique pour enrichir votre pause." },
                        lang = "fr",
                        thumbnail = if (coverImage.isNotEmpty()) com.example.data.models.WikiImage(source = coverImage) else null,
                        contentUrls = com.example.data.models.WikiContentUrls(
                            desktop = com.example.data.models.WikiUrl(page = articleUrl),
                            mobile = com.example.data.models.WikiUrl(page = articleUrl)
                        )
                    )
                    Result.success(summary)
                } else {
                    Result.failure(Exception("Empty Dev.to articles"))
                }
            } else {
                Result.failure(Exception("Dev.to HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchRandomSummary(lang: String, domain: String = "wikipedia.org"): Result<WikiSummaryResponse> {
        return try {
            val url = "https://$lang.$domain/api/rest_v1/page/random/summary"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ScrollBreak/1.0 (Android App; contact@scrollbreak.app)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val parsed = summaryAdapter.fromJson(body)
                if (parsed != null && parsed.extract.isNotBlank()) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Empty article response"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun searchTopic(lang: String, topic: String, domain: String = "wikipedia.org"): Result<List<String>> {
        return try {
            // Expand search query based on topic for richer content results
            val queryTerm = when (topic.lowercase().trim()) {
                "technology", "technologie", "technologies" -> if (lang == "fr") "Technologie OR Informatique OR Innovation OR Robotique OR Intelligence_artificielle" else "Technology OR Computing OR Innovation OR Robotics OR Artificial_intelligence"
                "science", "sciences" -> if (lang == "fr") "Science OR Physique OR Chimie OR Astronomie OR Biologie" else "Science OR Physics OR Chemistry OR Astronomy OR Biology"
                "history", "histoire" -> if (lang == "fr") "Histoire OR Civilisation OR Révolution OR Empire" else "History OR Civilization OR Revolution OR Empire"
                "nature", "environnement" -> if (lang == "fr") "Ecologie OR Biodiversity OR Wildlife OR Oceans OR Ecosystème" else "Nature OR Ecology OR Biodiversity OR Wildlife OR Ecosystem"
                "art", "culture" -> if (lang == "fr") "Art OR Peinture OR Architecture OR Sculpture OR Musique" else "Art OR Painting OR Architecture OR Sculpture OR Music"
                "space", "espace" -> if (lang == "fr") "Astronomie OR Cosmos OR Galaxie OR Exploration_spatiale" else "Space OR Astronomy OR Cosmos OR Galaxy OR Space_exploration"
                "philosophy", "philosophie" -> if (lang == "fr") "Philosophie OR Logique OR Ethique" else "Philosophy OR Logic OR Ethics"
                "psychology", "psychologie" -> if (lang == "fr") "Psychologie OR Cerveau OR Neurosciences" else "Psychology OR Brain OR Neuroscience"
                "health", "santé" -> if (lang == "fr") "Médecine OR Santé OR Biologie_humaine" else "Medicine OR Health OR Human_biology"
                else -> topic
            }

            val encodedTopic = URLEncoder.encode(queryTerm, "UTF-8")
            val url = "https://$lang.$domain/w/api.php?action=query&list=search&srsearch=$encodedTopic&srlimit=40&format=json&origin=*"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ScrollBreak/1.0 (Android App; contact@scrollbreak.app)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val parsed = searchAdapter.fromJson(body)
                val rawTitles = parsed?.query?.search?.map { it.title } ?: emptyList()

                // Filter out lists, portals, disambiguations
                val filtered = rawTitles.filter { title ->
                    val lower = title.lowercase()
                    !lower.startsWith("list of") &&
                    !lower.startsWith("liste de") &&
                    !lower.startsWith("category:") &&
                    !lower.startsWith("catégorie:") &&
                    !lower.startsWith("portal:") &&
                    !lower.startsWith("portail:") &&
                    !lower.contains("disambiguation") &&
                    !lower.contains("homonymie") &&
                    !lower.startsWith("index of")
                }

                val finalTitles = if (filtered.isNotEmpty()) filtered else rawTitles
                Result.success(finalTitles)
            } else {
                Result.failure(Exception("Search failed HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchArticleByTitle(lang: String, title: String, domain: String = "wikipedia.org"): Result<WikiSummaryResponse> = withContext(Dispatchers.IO) {
        fetchSummaryByTitle(lang.lowercase().trim(), title, domain)
    }

    private fun fetchSummaryByTitle(lang: String, title: String, domain: String = "wikipedia.org"): Result<WikiSummaryResponse> {
        return try {
            val encodedTitle = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
            val url = "https://$lang.$domain/api/rest_v1/page/summary/$encodedTitle"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ScrollBreak/1.0 (Android App; contact@scrollbreak.app)")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrEmpty()) {
                val parsed = summaryAdapter.fromJson(body)
                if (parsed != null && parsed.extract.isNotBlank()) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Empty summary for title"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFallbackArticle(lang: String, topic: String?): WikiSummaryResponse {
        return when (lang) {
            "fr" -> WikiSummaryResponse(
                title = "La Voie Lactée & L'Univers",
                description = "Astronomie & Connaissance",
                extract = "La Voie lactée est la galaxie dans laquelle se situe le Système solaire. Elle contient entre 200 et 400 milliards d'étoiles et au moins autant de planètes. Prenez un instant pour contempler l'immensité du cosmos plutôt que de faire défiler vos réseaux sociaux.",
                lang = "fr"
            )
            "es" -> WikiSummaryResponse(
                title = "El Viaje de la Mente Humana",
                description = "Ciencia y Filosofía",
                extract = "La neuroplasticidad es la capacidad del cerebro para formar nuevas conexiones neuronales a lo largo de la vida. Cambiar tus hábitos de navegación digital ayuda a fortalecer la atención consciente y la creatividad.",
                lang = "es"
            )
            "de" -> WikiSummaryResponse(
                title = "Achtsamkeit und Wissen",
                description = "Philosophie & Natur",
                extract = "Achtsamkeit bezeichnet einen Zustand von aufmerksamer Präsenz im gegenwärtigen Augenblick. Eine bewusste Pause mit Wissen erweitert den Horizont und stärkt die Konzentration.",
                lang = "de"
            )
            else -> WikiSummaryResponse(
                title = "The Power of Focused Attention",
                description = "Cognitive Science & Neuroscience",
                extract = "Neuroplasticity is the brain's ability to reorganize itself by forming new neural connections throughout life. Pausing mindless scrolling to engage with structured knowledge strengthens deep focus, working memory, and creative problem solving.",
                lang = "en"
            )
        }
    }
}
