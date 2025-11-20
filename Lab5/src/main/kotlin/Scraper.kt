package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
// Importujemy klasy z pakietu java.util.concurrent
// Kotlin nie ma własnych implementacji kolekcji współbieżnych,
// w pełni polega na tych dostarczanych przez JVM (Java).
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


// Ta funkcja pomocnicza pozostaje bez zmian.
private fun buildWikipediaUrl(articleTitle: String): String {
    val formattedTerm = articleTitle.replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

// Ta funkcja również pozostaje bez zmian.
// Jest "thread-safe" (bezpieczna wątkowo), ponieważ nie modyfikuje
// żadnego stanu współdzielonego - operuje tylko na lokalnych zmiennych.
private fun extractArticleLinks(articleTitle: String): Set<String> {
    val foundLinks = mutableSetOf<String>()
    val maxLinksPerPage = 20 // OGRANICZENIE
    try {
        val url = buildWikipediaUrl(articleTitle)
        val doc = Jsoup.connect(url).get()

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
 * NOWA WERSJA - oparta na ParallelStream
 * Główna funkcja scrapująca. Realizuje algorytm BFS z synchronizacją poziomów.
 *
 * @param startTerm Tytuł artykułu startowego.
 * @param maxDepth Maksymalna głębokość przeszukiwania.
 * @return Mapa, gdzie kluczem jest poziom głębokości, a wartością zbiór tytułów artykułów.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {

    // --- BEZPIECZNE WĄTKOWO KOLEKCJE ---
    // Nadal ich potrzebujemy, ponieważ wiele wątków ze strumienia
    // będzie JEDNOCZEŚNIE zapisywać dane.

    // Mapa na wyniki
    val results = ConcurrentHashMap<Int, MutableSet<String>>()
    // Zbiór odwiedzonych (gwarantuje atomowość operacji .add())
    val visited = ConcurrentHashMap.newKeySet<String>()

    // Lista artykułów do przetworzenia na BIEŻĄCYM poziomie.
    // To jest nasza "partia" pracy.
    var currentLevelArticles = listOf(startTerm)

    // Dodajemy element startowy
    visited.add(startTerm)

    println("Rozpoczynam przeszukiwanie (ParallelStream)...")

    // Główna pętla iteruje po poziomach głębokości (1, 2, ... maxDepth)
    for (currentDepth in 1..maxDepth) {
        println("\n--- Przetwarzanie poziomu $currentDepth ---")

        // Kolejka na linki znalezione na tym poziomie (będą partią pracy na następny poziom)
        // Musi być thread-safe, bo wiele wątków będzie do niej dodawać.
        val nextLevelQueue = ConcurrentLinkedQueue<String>()
        // Przygotowujemy zbiór na wyniki dla bieżącego poziomu
        results[currentDepth] = ConcurrentHashMap.newKeySet<String>()

        println("Uruchamianie strumienia równoległego dla ${currentLevelArticles.size} artykułów...")

        // --- MAGIA PARALLELSTREAM ---
        // 1. .parallelStream() - konwertuje listę artykułów na strumień,
        //    który będzie przetwarzany równolegle.
        // 2. .forEach { ... } - to jest "operacja terminalna". Blokuje ona
        //    wątek główny do czasu, aż WSZYSTKIE elementy w strumieniu
        //    zostaną przetworzone przez pulę wątków.
        //    Działa to jak NIEJAWNY .join() na wszystkich zadaniach.
        currentLevelArticles.parallelStream().forEach { articleTitle ->
            // Ten blok kodu jest wykonywany RÓWNOLEGLE przez wiele wątków
            // dla różnych 'articleTitle'
            try {
                // 1. Pobierz linki (operacja I/O)
                val newLinks = extractArticleLinks(articleTitle)

                // 2. Przetwórz znalezione linki
                for (linkTitle in newLinks) {
                    // 3. Atomowo sprawdź, czy link był odwiedzony, i dodaj go
                    if (visited.add(linkTitle)) {
                        // Jeśli to nowy link, dodajemy go do współbieżnej kolejki
                        nextLevelQueue.add(linkTitle)
                        // I zapisujemy go we współbieżnym zbiorze wyników
                        results[currentDepth]?.add(linkTitle.replace('_', ' '))
                    }
                }
            } catch (e: Exception) {
                println("!! Błąd w wątku (strumień) dla $articleTitle: ${e.message}")
            }
        } // --- KONIEC BLOKU PARALLELSTREAM.FOREACH ---
        // Wątek główny wznawia pracę dopiero TUTAJ,
        // gdy wszystkie artykuły z `currentLevelArticles` są przetworzone.

        println("Wszystkie zadania strumienia dla poziomu $currentDepth zakończone. Znaleziono ${nextLevelQueue.size} nowych unikalnych linków.")

        // Jeśli kolejka na następny poziom jest pusta, przerywamy
        if (nextLevelQueue.isEmpty()) {
            println("Brak nowych linków, kończenie przeszukiwania.")
            break
        }

        // Przygotowanie do następnej iteracji:
        // Konwertujemy współbieżną kolejkę na zwykłą listę,
        // która będzie "partią pracy" dla następnej pętli.
        currentLevelArticles = nextLevelQueue.toList()
    }

    println("Zakończono przeszukiwanie.")
    return results
}