package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WikiSummaryResponse(
    val title: String = "",
    val displaytitle: String? = null,
    val description: String? = null,
    val extract: String = "",
    @Json(name = "extract_html") val extractHtml: String? = null,
    val thumbnail: WikiImage? = null,
    val originalimage: WikiImage? = null,
    val lang: String = "en",
    val dir: String = "ltr",
    val pageid: Long? = null,
    @Json(name = "content_urls") val contentUrls: WikiContentUrls? = null
) {
    fun getPageUrl(): String {
        val mobileUrl = contentUrls?.mobile?.page
        if (!mobileUrl.isNullOrEmpty()) return mobileUrl
        val desktopUrl = contentUrls?.desktop?.page
        if (!desktopUrl.isNullOrEmpty()) return desktopUrl
        return try {
            val encoded = java.net.URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
            "https://$lang.m.wikipedia.org/wiki/$encoded"
        } catch (e: Exception) {
            "https://$lang.m.wikipedia.org/wiki/${title.replace(" ", "_")}"
        }
    }
}

@JsonClass(generateAdapter = true)
data class WikiImage(
    val source: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@JsonClass(generateAdapter = true)
data class WikiContentUrls(
    val desktop: WikiUrl? = null,
    val mobile: WikiUrl? = null
)

@JsonClass(generateAdapter = true)
data class WikiUrl(
    val page: String = "",
    val edit: String = "",
    val talk: String = ""
)

// Search response model for Wikipedia MediaWiki API
@JsonClass(generateAdapter = true)
data class WikiSearchResponse(
    val query: WikiQuery? = null
)

@JsonClass(generateAdapter = true)
data class WikiQuery(
    val search: List<WikiSearchResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WikiSearchResult(
    val title: String = "",
    val pageid: Long = 0,
    val snippet: String = ""
)
