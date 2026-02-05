package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Buduje poprawny adres URL do artykułu w polskiej Wikipedii.
 * @param articleTitle Tytuł artykułu.
 * @return Pełny adres URL.
 */
private fun buildWikipediaUrl(articleTitle: String): String {
    val formattedTerm = articleTitle.trim().replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

/**
 * Pobiera linki do innych artykułów z podanej strony Wikipedii.
 *
 * @param articleTitle Tytuł artykułu do przeskanowania.
 * @return Zbiór unikalnych tytułów artykułów znalezionych na stronie.
 */
private fun extractArticleLinks(articleTitle: String): Set<String> {
    val foundLinks = mutableSetOf<String>()
    val maxLinksPerPage = 50
    try {
        val url = buildWikipediaUrl(articleTitle)
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .timeout(5000)
            .get()
        val links = doc.select("#mw-content-text a[href]")

        var count = 0
        val it = links.iterator()
        while (it.hasNext() && count < maxLinksPerPage) {
            val link = it.next()
            val href = link.attr("href")

            if (href.startsWith("/wiki/") && !href.contains(":") && href != "/wiki/Strona_główna") {
                val title = href.substringAfter("/wiki/")
                val decodedTitle = URLDecoder.decode(title, "UTF-8")
                
                if (foundLinks.add(decodedTitle)) {
                    count++
                }
            }
        }
    } catch (e: HttpStatusException) {
        // Ignorujemy błędy 404
    } catch (e: Exception) {
        println("! Błąd podczas pobierania $articleTitle: ${e.message}")
    }
    return foundLinks
}

/**
 * WERSJA LAB 7: CompletableFuture
 * Wykorzystuje asynchroniczne programowanie funkcyjne.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {
    val results = ConcurrentHashMap<Int, MutableSet<String>>()
    val visited = ConcurrentHashMap.newKeySet<String>()
    
    var currentLevelArticles: List<String> = listOf(startTerm)
    visited.add(startTerm)

    println("Rozpoczynam przeszukiwanie (Lab 7: CompletableFuture)...")

    for (currentDepth in 1..maxDepth) {
        println("\n--- Przetwarzanie poziomu $currentDepth ---")
        
        val nextLevelResults = ConcurrentHashMap.newKeySet<String>()
        results[currentDepth] = nextLevelResults
        
        // Tworzymy listę asynchronicznych zadań
        val futures = currentLevelArticles.map { articleTitle: String ->
            CompletableFuture.supplyAsync {
                extractArticleLinks(articleTitle)
            }.thenAccept { newLinks: Set<String> ->
                for (link in newLinks) {
                    if (visited.add(link)) {
                        nextLevelResults.add(link.replace('_', ' '))
                    }
                }
            }
        }

        // Czekamy na wszystkie zadania
        if (futures.isNotEmpty()) {
            val futuresArray = futures.toTypedArray()
            CompletableFuture.allOf(*futuresArray).join()
        }

        // Używamy jawnej lambdy, aby uniknąć problemów z 'it'
        currentLevelArticles = nextLevelResults.map { title: String -> title.replace(' ', '_') }
        println("Poziom $currentDepth zakończony. Znaleziono ${currentLevelArticles.size} linków.")

        if (currentLevelArticles.isEmpty()) break
    }

    println("Zakończono przeszukiwanie.")
    return results
}