package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
// Nadal potrzebujemy tych dwóch do wydajnej obsługi `visited` i kolejki
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
// ArrayDeque nie jest już nam potrzebny, bo zaczynamy od Listy
// import java.util.ArrayDeque

// Funkcje buildWikipediaUrl i extractArticleLinks pozostają bez zmian
private fun buildWikipediaUrl(articleTitle: String): String {
    val formattedTerm = articleTitle.replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

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
 * WERSJA LAB 3: Wątki + Ręczna Synchronizacja
 *
 * Używamy ręcznie tworzonych wątków (jak w Lab 2) oraz `thread.join()`,
 * ale dodajemy ręczną synchronizację (mutual exclusion) dla mapy `results`
 * za pomocą bloku `synchronized`.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {

    // --- ZMIANA LAB 3 ---
    // Zamiast ConcurrentHashMap, tworzymy ZWYKŁĄ mapę.
    // NIE JEST ONA BEZPIECZNA WĄTKOWO!
    val results = mutableMapOf<Int, MutableSet<String>>()

    // Tworzymy "zamek" (monitor). To dowolny obiekt, który posłuży
    // jako "strażnik" dostępu do naszej mapy `results`.
    val resultsLock = Any()

    // Dla `visited` i kolejek nadal używamy klas współbieżnych.
    // `visited` - bo .add() jest atomowe, co jest super wydajne.
    // `nextLevelQueue` - bo wiele wątków dodaje do niej elementy.
    // Mieszanie tych podejść jest dobrą praktyką - nie blokujemy
    // wszystkiego jednym wielkim zamkiem, co zabiłoby wydajność.
    val visited = ConcurrentHashMap.newKeySet<String>()
    var currentLevelQueue = mutableListOf(startTerm)

    visited.add(startTerm)
    println("Rozpoczynam przeszukiwanie (Lab 3: Wątki + Ręczna Synchronizacja)...")

    // Pętla iteruje po poziomach głębokości
    for (currentDepth in 1..maxDepth) {
        println("\n--- Przetwarzanie poziomu $currentDepth ---")

        val nextLevelQueue = ConcurrentLinkedQueue<String>()
        val threads = mutableListOf<Thread>()

        // --- ZMIANA LAB 3 ---
        // Tworzymy zwykły, niesynchronizowany zbiór na wyniki DLA TEGO POZIOMU.
        val newResultSetForThisLevel = mutableSetOf<String>()

        // Musimy dodać ten nowy zbiór do mapy `results`.
        // Ponieważ `results` nie jest bezpieczna wątkowo, robimy to
        // w bloku synchronized, zanim jeszcze uruchomimy wątki.
        // Mógłby to też robić jeden z wątków, ale tak jest czyściej.
        synchronized(resultsLock) {
            results[currentDepth] = newResultSetForThisLevel
        }

        // Przechodzimy przez wszystkie artykuły do przetworzenia na tym poziomie
        for (articleTitle in currentLevelQueue) {

            // Tworzymy zadanie (Runnable) dla wątku
            val task = Runnable {
                try {
                    val newLinks = extractArticleLinks(articleTitle)
                    for (linkTitle in newLinks) {

                        // Atomowe sprawdzenie i dodanie do `visited`.
                        // To jest bezpieczne wątkowo bez naszego zamka.
                        if (visited.add(linkTitle)) {

                            // Dodanie do kolejki na następny poziom.
                            // To też jest bezpieczne wątkowo bez naszego zamka.
                            nextLevelQueue.add(linkTitle)

                            // --- ZMIANA LAB 3: SEKCJA KRYTYCZNA ---
                            // Teraz chcemy dodać link do zbioru wyników.
                            // `newResultSetForThisLevel` NIE JEST bezpieczny wątkowo.
                            // Musimy "poprosić o klucz" (zamek `resultsLock`).
                            synchronized(resultsLock) {
                                // Ten blok kodu wykona tylko JEDEN wątek na raz.
                                // Inne wątki, które tu dotrą, będą czekać w kolejce.
                                // Gdy mamy zamek, możemy bezpiecznie modyfikować
                                // zbiór `newResultSetForThisLevel`.
                                newResultSetForThisLevel.add(linkTitle.replace('_', ' '))

                            } // Wątek automatycznie zwalnia zamek po wyjściu z bloku.
                        }
                    }
                } catch (e: Exception) {
                    println("!! Błąd w wątku dla $articleTitle: ${e.message}")
                }
            } // Koniec definicji zadania (Runnable)

            // Tworzenie i uruchamianie wątku (jak w Lab 2)
            val thread = Thread(task)
            threads.add(thread)
            thread.start()
        }

        println("Uruchomiono ${threads.size} wątków. Oczekiwanie na .join()...")

        // Synchronizacja na ZAKOŃCZENIE (jak w Lab 2)
        for (thread in threads) {
            try {
                thread.join()
            } catch (e: InterruptedException) {
                println("!! Wątek główny przerwany: ${e.message}")
            }
        }

        println("Wszystkie wątki poziomu $currentDepth zakończone. Znaleziono ${nextLevelQueue.size} nowych unikalnych linków.")

        if (nextLevelQueue.isEmpty()) {
            println("Brak nowych linków, kończenie przeszukiwania.")
            break
        }

        currentLevelQueue = nextLevelQueue.toMutableList()
    }

    println("Zakończono przeszukiwanie.")
    return results // Zwracamy naszą ręcznie synchronizowaną mapę
}