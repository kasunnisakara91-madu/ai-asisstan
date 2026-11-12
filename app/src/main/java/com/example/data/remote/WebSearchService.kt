package com.example.data.remote

import android.text.Html
import com.example.ai.ConfigurationRequiredException
import com.example.data.local.preferences.SettingsRepository
import com.example.data.local.preferences.WebSearchProvider
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.net.URI
import java.util.concurrent.TimeUnit

data class WebSearchResultItem(
    val title: String,
    val url: String,
    val snippet: String,
    val sourceLabel: String
)

data class WebSearchResponse(
    val provider: WebSearchProvider,
    val query: String,
    val results: List<WebSearchResultItem>
)

@JsonClass(generateAdapter = true)
data class WikiApiResponse(val query: WikiQuery? = null)

@JsonClass(generateAdapter = true)
data class WikiQuery(val search: List<WikiSearchHit> = emptyList())

@JsonClass(generateAdapter = true)
data class WikiSearchHit(
    val title: String = "",
    val pageid: Long = 0L,
    val snippet: String = ""
)

@JsonClass(generateAdapter = true)
data class GoogleCseResponse(val items: List<GoogleCseItem>? = null)

@JsonClass(generateAdapter = true)
data class GoogleCseItem(
    val title: String = "",
    val link: String = "",
    val snippet: String = "",
    val displayLink: String = "Web"
)

@JsonClass(generateAdapter = true)
data class SerperRequest(val q: String, val num: Int = 6)

@JsonClass(generateAdapter = true)
data class SerperResponse(val organic: List<SerperHit>? = null)

@JsonClass(generateAdapter = true)
data class SerperHit(
    val title: String = "",
    val link: String = "",
    val snippet: String = ""
)

@JsonClass(generateAdapter = true)
data class TavilyRequest(
    val api_key: String,
    val query: String,
    val max_results: Int = 6,
    val search_depth: String = "basic"
)

@JsonClass(generateAdapter = true)
data class TavilyResponse(val results: List<TavilyHit>? = null)

@JsonClass(generateAdapter = true)
data class TavilyHit(
    val title: String = "",
    val url: String = "",
    val content: String = ""
)

interface WebSearchRetrofitApi {
    @Headers("User-Agent: AI-ASISSTANT-Android/1.0 (https://github.com/ai-assisstant)")
    @GET("https://en.wikipedia.org/w/api.php")
    suspend fun searchWikipedia(
        @Query("action") action: String = "query",
        @Query("list") list: String = "search",
        @Query("srsearch") srsearch: String,
        @Query("srlimit") srlimit: Int = 6,
        @Query("utf8") utf8: Int = 1,
        @Query("format") format: String = "json"
    ): WikiApiResponse

    @GET("https://www.googleapis.com/customsearch/v1")
    suspend fun searchGoogleCse(
        @Query("key") key: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("num") num: Int = 6
    ): GoogleCseResponse

    @POST
    suspend fun searchSerper(
        @Url url: String = "https://google.serper.dev/search",
        @Header("X-API-KEY") apiKey: String,
        @Body body: SerperRequest
    ): SerperResponse

    @POST
    suspend fun searchTavily(
        @Url url: String = "https://api.tavily.com/search",
        @Body body: TavilyRequest
    ): TavilyResponse
}

class WebSearchRepository(
    private val settingsRepository: SettingsRepository
) {
    private val api: WebSearchRetrofitApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WebSearchRetrofitApi::class.java)
    }

    suspend fun search(query: String): WebSearchResponse = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        require(trimmed.isNotEmpty()) { "Enter a search query first." }

        val settings = settingsRepository.settingsFlow.first()
        val apiKey = settingsRepository.getDecryptedWebSearchApiKey()

        withTimeout(25_000L) {
            when (settings.webSearchProvider) {
                WebSearchProvider.WIKIPEDIA_LIVE -> {
                    val res = api.searchWikipedia(srsearch = trimmed)
                    val items = res.query?.search.orEmpty().mapNotNull { hit ->
                        if (hit.title.isBlank() || hit.pageid <= 0L) return@mapNotNull null
                        val cleanSnippet = Html.fromHtml(hit.snippet, Html.FROM_HTML_MODE_COMPACT)
                            .toString()
                            .trim()
                        WebSearchResultItem(
                            title = hit.title,
                            url = "https://en.wikipedia.org/?curid=${hit.pageid}",
                            snippet = cleanSnippet,
                            sourceLabel = "Wikipedia"
                        )
                    }
                    WebSearchResponse(WebSearchProvider.WIKIPEDIA_LIVE, trimmed, items)
                }

                WebSearchProvider.GOOGLE_CUSTOM_SEARCH -> {
                    if (apiKey.isBlank() || settings.webSearchEngineId.isBlank()) {
                        throw ConfigurationRequiredException(
                            "Google Custom Search requires both an API Key and Search Engine ID (cx) in Settings → Web Search API Configuration."
                        )
                    }
                    val res = api.searchGoogleCse(
                        key = apiKey,
                        cx = settings.webSearchEngineId,
                        query = trimmed
                    )
                    val items = res.items.orEmpty().map {
                        WebSearchResultItem(
                            title = it.title,
                            url = it.link,
                            snippet = it.snippet,
                            sourceLabel = it.displayLink.ifBlank { "Web" }
                        )
                    }
                    WebSearchResponse(WebSearchProvider.GOOGLE_CUSTOM_SEARCH, trimmed, items)
                }

                WebSearchProvider.SERPER_DEV -> {
                    if (apiKey.isBlank()) {
                        throw ConfigurationRequiredException(
                            "Serper.dev requires an API key in Settings → Web Search API Configuration."
                        )
                    }
                    val res = api.searchSerper(
                        apiKey = apiKey,
                        body = SerperRequest(q = trimmed)
                    )
                    val items = res.organic.orEmpty().map {
                        WebSearchResultItem(
                            title = it.title,
                            url = it.link,
                            snippet = it.snippet,
                            sourceLabel = extractHost(it.link)
                        )
                    }
                    WebSearchResponse(WebSearchProvider.SERPER_DEV, trimmed, items)
                }

                WebSearchProvider.TAVILY_AI -> {
                    if (apiKey.isBlank()) {
                        throw ConfigurationRequiredException(
                            "Tavily AI requires an API key in Settings → Web Search API Configuration."
                        )
                    }
                    val res = api.searchTavily(
                        body = TavilyRequest(api_key = apiKey, query = trimmed)
                    )
                    val items = res.results.orEmpty().map {
                        WebSearchResultItem(
                            title = it.title,
                            url = it.url,
                            snippet = it.content,
                            sourceLabel = extractHost(it.url)
                        )
                    }
                    WebSearchResponse(WebSearchProvider.TAVILY_AI, trimmed, items)
                }
            }
        }
    }

    private fun extractHost(url: String): String =
        runCatching { URI(url).host ?: "Web" }.getOrDefault("Web")
}
