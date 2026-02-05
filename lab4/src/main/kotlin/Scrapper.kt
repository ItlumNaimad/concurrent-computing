package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
// Nadal potrzebujemy tych dwóch do wydajnej obsługi `visited` i kolejki
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Funkcje buildWikipediaUrl i extractArticleLinks pozostają bez zmian
private fun buildWikipediaUrl(articleTitle: String): String {
    val formattedTerm = articleTitle.trim().replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

private fun extractArticleLinks(articleTitle: String): Set<String> {
    val foundLinks = mutableSetOf<String>()
    val maxLinksPerPage = 50 // OGRANICZENIE
    try {
        val url = buildWikipediaUrl(articleTitle)
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()
        val links = doc.select("#mw-content-text a[href]")

        for (link in links.take(maxLinksPerPage * 2)) {
            val href = link.attr("href")
            if (href.startsWith("/wiki/") && !href.contains(":") && href != "/wiki/Strona_główna") {
                val title = href.substringAfter("/wiki/")
                val decodedTitle = URLDecoder.decode(title, "UTF-8")
                if (foundLinks.size < maxLinksPerPage) {
                    foundLinks.add(decodedTitle)
                } else {
                    break
                }
            }
        }
    } catch (e: HttpStatusException) {
        println("! Nie można odnaleźć strony dla: $articleTitle (błąd ${e.statusCode})")
    } catch (e: Exception) {
        println("! Wystąpił błąd podczas przetwarzania $articleTitle: ${e.message}")
    }
    return foundLinks
}


/**
 * WERSJA LAB 4: Pula Wątków (ExecutorService)
 *
 * Wykorzystujemy ExecutorService do zarządzania pulą wątków. Zamiast tworzyć
 * nowy wątek dla każdego zadania, delegujemy je do puli, co oszczędza zasoby.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {

    // Używamy ConcurrentHashMap dla bezpieczeństwa wątkowego
    val results = ConcurrentHashMap<Int, MutableSet<String>>()
    val visited = ConcurrentHashMap.newKeySet<String>()
    var currentLevelQueue = mutableListOf(startTerm)

    visited.add(startTerm)

    // Tworzymy pulę wątków o rozmiarze zależnym od liczby rdzeni procesora
    val poolSize = Runtime.getRuntime().availableProcessors() * 2
    val executor = Executors.newFixedThreadPool(poolSize)

    println("Rozpoczynam przeszukiwanie (Lab 4: Pula Wątków - $poolSize wątków)...")

    try {
        // Pętla iteruje po poziomach głębokości
        for (currentDepth in 1..maxDepth) {
            println("\n--- Przetwarzanie poziomu $currentDepth ---")

            val nextLevelQueue = ConcurrentLinkedQueue<String>()
            results[currentDepth] = ConcurrentHashMap.newKeySet<String>()

            // Przekazujemy zadania do puli wątków i zbieramy obiekty Future
            val futures = currentLevelQueue.map { articleTitle ->
                executor.submit {
                    try {
                        val newLinks = extractArticleLinks(articleTitle)
                        for (linkTitle in newLinks) {
                            if (visited.add(linkTitle)) {
                                nextLevelQueue.add(linkTitle)
                                results[currentDepth]?.add(linkTitle.replace('_', ' '))
                            }
                        }
                    } catch (e: Exception) {
                        println("!! Błąd w zadaniu dla $articleTitle: ${e.message}")
                    }
                }
            }

            // Oczekiwanie na zakończenie wszystkich zadań z bieżącego poziomu (bariera)
            for (future in futures) {
                try {
                    future.get() // Blokuje do momentu zakończenia zadania
                } catch (e: Exception) {
                    println("!! Błąd podczas oczekiwania na zadanie: ${e.message}")
                }
            }

            println("Zadania poziomu $currentDepth zakończone. Znaleziono ${nextLevelQueue.size} nowych unikalnych linków.")

            if (nextLevelQueue.isEmpty()) break
            currentLevelQueue = nextLevelQueue.toMutableList()
        }
    } finally {
        // Bardzo ważne: Zamknięcie puli wątków po zakończeniu pracy
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }

    println("Zakończono przeszukiwanie.")
    return results
}