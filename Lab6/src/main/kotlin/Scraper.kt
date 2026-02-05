package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

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
 * Funkcja jest bezpieczna wątkowo, ponieważ nie modyfikuje stanu współdzielonego.
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

        for (link in links.take(maxLinksPerPage * 2)) {
            val href = link.attr("href")
            if (href.startsWith("/wiki/") && !href.contains(":") && href != "/wiki/Strona_główna") {
                val title = href.substringAfter("/wiki/")
                val decodedTitle = URLDecoder.decode(title, "UTF-8")
                if (foundLinks.size < maxLinksPerPage) {
                    foundLinks.add(decodedTitle)
                } else break
            }
        }
    } catch (e: HttpStatusException) {
        // Ignorujemy błędy HTTP 404 itp.
    } catch (e: Exception) {
        println("! Błąd podczas pobierania $articleTitle: ${e.message}")
    }
    return foundLinks
}

/**
 * WERSJA LAB 6: Kolejka Blokująca (BlockingQueue)
 * Implementacja wzorca Producent-Konsument do równoległego przeszukiwania Wikipedii.
 *
 * @param startTerm Tytuł artykułu startowego.
 * @param maxDepth Maksymalna głębokość przeszukiwania.
 * @return Mapa wyników pogrupowana według poziomów głębokości.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {
    // Wyniki przechowywane w bezpiecznej mapie współbieżnej
    val results = ConcurrentHashMap<Int, MutableSet<String>>()
    // Zbiór odwiedzonych linków (atomowe operacje .add())
    val visited = ConcurrentHashMap.newKeySet<String>()
    
    // Kolejka blokująca przechowująca zadania do wykonania (tytuł i aktualna głębokość)
    // Jest to serce wzorca producent-konsument.
    val queue = LinkedBlockingQueue<Pair<String, Int>>()
    
    // Pula wątków działająca jako konsumenci zadań z kolejki
    val poolSize = Runtime.getRuntime().availableProcessors() * 2
    val executor = Executors.newFixedThreadPool(poolSize)
    
    // Licznik aktywnych zadań, aby wiedzieć, kiedy zakończyć pracę
    val activeTasks = AtomicInteger(0)

    println("Rozpoczynam przeszukiwanie (Lab 6: Kolejka Blokująca - $poolSize wątków)...")

    // Inicjalizacja: dodanie pierwszego artykułu
    visited.add(startTerm)
    queue.put(startTerm to 0)
    activeTasks.incrementAndGet()

    // Logika zarządzająca: dopóki są aktywne zadania lub coś jest w kolejce
    while (activeTasks.get() > 0) {
        // Pobieramy zadanie z kolejki z timeoutem
        val task = queue.poll(500, TimeUnit.MILLISECONDS)
        
        if (task != null) {
            executor.execute {
                val (currentTitle, currentDepth) = task
                
                // Jeśli nie osiągnęliśmy jeszcze maksymalnej głębokości, szukamy dalej
                if (currentDepth < maxDepth) {
                    val newLinks = extractArticleLinks(currentTitle)
                    
                    for (link in newLinks) {
                        // Jeśli link jest nowy (nieodwiedzony)
                        if (visited.add(link)) {
                            val nextDepth = currentDepth + 1
                            
                            // Zapisujemy wynik (bezpiecznie wątkowo)
                            results.computeIfAbsent(nextDepth) { ConcurrentHashMap.newKeySet() }
                                .add(link.replace('_', ' '))
                            
                            // Jeśli możemy szukać głębiej, dodajemy link jako nowe zadanie do kolejki
                            if (nextDepth < maxDepth) {
                                activeTasks.incrementAndGet()
                                queue.put(link to nextDepth)
                            }
                        }
                    }
                }
                // Zadanie wykonane - zmniejszamy licznik
                activeTasks.decrementAndGet()
            }
        }
    }

    // Zamknięcie puli wątków
    executor.shutdown()
    try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow()
        }
    } catch (e: InterruptedException) {
        executor.shutdownNow()
    }

    println("Zakończono przeszukiwanie.")
    return results
}
