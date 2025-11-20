package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

// Funkcja buildWikipediaUrl do budowania linków wikipedii (podmienia znaki spacji)
private fun buildWikipediaUrl(articleTitle: String): String {
    val formattedTerm = articleTitle.replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

// Funkcja extractArticleLinks do pobierania linków z artykułu
// Jest "thread-safe", ponieważ operuje tylko na własnych, lokalnych zmiennych
// i nie modyfikuje żadnego stanu współdzielonego.
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
 * NOWA WERSJA - WIELOWĄTKOWA
 * Główna funkcja scrapująca. Realizuje algorytm BFS z synchronizacją poziomów.
 *
 * @param startTerm Tytuł artykułu startowego.
 * @param maxDepth Maksymalna głębokość przeszukiwania.
 * @return Mapa, gdzie kluczem jest poziom głębokości, a wartością zbiór tytułów artykułów.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {

    // --- BEZPIECZNE WĄTKOWO KOLEKCJE ---
    // Mapa na wyniki. Używamy ConcurrentHashMap, aby wiele wątków mogło
    // bezpiecznie dodawać wyniki do wewnętrznych zbiorów.
    val results = ConcurrentHashMap<Int, MutableSet<String>>()

    // Zbiór odwiedzonych. Używamy ConcurrentHashMap.newKeySet()
    // Gwarantuje on, że operacja .add() jest atomowa.
    val visited = ConcurrentHashMap.newKeySet<String>()

    // Lista artykułów do przetworzenia na BIEŻĄCYM poziomie głębokości.
    var currentLevelQueue = mutableListOf(startTerm)

    // Dodajemy element startowy do odwiedzonych
    visited.add(startTerm)

    println("Rozpoczynam przeszukiwanie wielowątkowe...")

    // Pętla iteruje po poziomach głębokości
    for (currentDepth in 1..maxDepth) {
        println("\n--- Przetwarzanie poziomu $currentDepth ---")

        // Kolejka na linki znalezione na tym poziomie (będą zadaniami na następny poziom)
        // Musi być thread-safe, bo wiele wątków będzie do niej dodawać elementy.
        val nextLevelQueue = ConcurrentLinkedQueue<String>()

        // Lista do przechowywania wszystkich wątków uruchomionych na tym poziomie
        // Musimy je śledzić, aby móc na nie poczekać (join).
        val threads = mutableListOf<Thread>()

        // Przygotowujemy bezpieczny wątkowo zbiór na wyniki dla tego poziomu
        results[currentDepth] = ConcurrentHashMap.newKeySet<String>()

        // Przechodzimy przez wszystkie artykuły do przetworzenia na tym poziomie
        for (articleTitle in currentLevelQueue) {

            // Tworzymy zadanie (Runnable), które zostanie wykonane przez nowy wątek
            val task = Runnable {
                // To jest kod, który wykona się w OSOBNYM wątku
                try {
                    // 1. Pobierz linki (operacja I/O - tutaj zyskujemy na współbieżności)
                    val newLinks = extractArticleLinks(articleTitle)

                    // 2. Przetwórz znalezione linki
                    for (linkTitle in newLinks) {
                        // 3. Atomowo sprawdź, czy link był odwiedzony, i dodaj go
                        //    Metoda .add() zwróci true tylko, jeśli elementu nie było
                        if (visited.add(linkTitle)) {
                            // Jeśli to nowy link, dodajemy go do kolejki na następny poziom
                            nextLevelQueue.add(linkTitle)
                            // I zapisujemy go w wynikach dla bieżącego poziomu
                            results[currentDepth]?.add(linkTitle.replace('_', ' '))
                        }
                    }
                } catch (e: Exception) {
                    println("!! Błąd w wątku dla $articleTitle: ${e.message}")
                }
            } // Koniec definicji zadania (Runnable)

            // Tworzymy nowy wątek z naszym zadaniem
            val thread = Thread(task)
            // Dodajemy go do naszej listy
            threads.add(thread)
            // Uruchamiamy wątek (zaczyna on wykonywać kod z bloku Runnable)
            thread.start()
        } // Koniec pętli tworzącej wątki

        println("Uruchomiono ${threads.size} wątków dla poziomu $currentDepth. Oczekiwanie na zakończenie...")

        // --- KLUCZOWY MOMENT - JOIN ---
        // Teraz wątek główny musi poczekać, aż WSZYSTKIE wątki robocze skończą.
        // Iterujemy po liście wątków, które uruchomiliśmy...
        for (thread in threads) {
            try {
                // ...i na każdym wywołujemy .join()
                // To polecenie blokuje WĄTEK GŁÓWNY aż ten konkretny 'thread'
                // nie zakończy swojego działania (bloku Runnable).
                thread.join()
            } catch (e: InterruptedException) {
                // Obsługa błędu przerwania wątku
                println("!! Wątek główny został przerwany podczas oczekiwania: ${e.message}")
            }
        }

        println("Wszystkie wątki poziomu $currentDepth zakończone. Znaleziono ${nextLevelQueue.size} nowych unikalnych linków.")

        // Jeśli na tym poziomie nie znaleźliśmy żadnych nowych linków, to nie ma sensu
        // kontynuować - przerywamy główną pętlę.
        if (nextLevelQueue.isEmpty()) {
            println("Brak nowych linków, kończenie przeszukiwania.")
            break
        }

        // Przygotowanie do następnej iteracji:
        // linki znalezione na tym poziomie stają się zadaniami na następny poziom.
        currentLevelQueue = nextLevelQueue.toMutableList()

    } // Koniec pętli po poziomach (for currentDepth...)

    println("Zakończono przeszukiwanie.")

    // Zwracamy mapę wyników. Nie musimy jej konwertować,
    // ConcurrentHashMap jest pełnoprawną implementacją interfejsu Map.
    return results
}