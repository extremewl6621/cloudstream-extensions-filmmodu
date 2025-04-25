import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class FilmmoduProvider : MainAPI() {
    override var mainUrl = "https://www.filmmodu.nl"
    override var name = "Filmmodu"
    override val hasMainPage = true
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Movie)

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/arama?q=$query"
        val doc = app.get(url).document

        return doc.select(".movies .movie-item").mapNotNull {
            val title = it.selectFirst(".movie-title")?.text() ?: return@mapNotNull null
            val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = it.selectFirst("img")?.attr("src")

            MovieSearchResponse(
                name = title,
                url = fixUrl(href),
                apiName = this.name,
                type = TvType.Movie,
                posterUrl = fixUrl(poster)
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content") ?: "Film"
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val iframe = doc.selectFirst("iframe")?.attr("src") ?: throw ErrorLoadingException("No iframe")

        val description = doc.selectFirst(".movie-description")?.text()
        val tags = doc.select(".movie-genres a").map { it.text() }

        return MovieLoadResponse(
            name = title,
            url = url,
            apiName = this.name,
            type = TvType.Movie,
            dataUrl = iframe,
            posterUrl = poster,
            plot = description,
            tags = tags
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val embedUrl = data
        val doc = app.get(embedUrl, referer = mainUrl).document

        val scriptTag = doc.selectFirst("script:containsData(sources)")?.data()

        val videoUrl = Regex("file["']?\s*:\s*["'](https?[^"']+)["']")
            .find(scriptTag ?: "")?.groupValues?.get(1)

        if (videoUrl != null) {
            callback(
                ExtractorLink(
                    name = "Filmmodu",
                    source = "filmmodu.nl",
                    url = videoUrl,
                    referer = embedUrl,
                    quality = Qualities.Unknown.value,
                    isM3u8 = videoUrl.endsWith(".m3u8")
                )
            )
        } else {
            throw ErrorLoadingException("Video link bulunamadı")
        }
    }
}