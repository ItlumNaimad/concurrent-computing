package org.example

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.ArrayDeque

/**
 * URLEncoder.encode zamienia spacje na '+' domyślnie, ale Wikipedia używa '_'.
 * Po zakodowaniu, możemy ręcznie zamienić '+' na '_', a jeśli trzeba też inne znaki.
 * Lepszym podejściem jest prosta zamiana spacji na podkreślnik, bo Wikipedia jest dość liberalna w tej kwestii.
 * @param: term :String
 * @return: url: String
 */

private fun buildWikipediaUrl(articleTitle: String): String {
    // Spacje zamieniamy na podkreślniki, to standard w URL-ach Wiki.
    val formattedTerm = articleTitle.replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}

/**
 * Funkcja pomocnicza, która dla danego tytułu artykułu pobiera stronę
 * i wyciąga z niej wszystkie unikalne linki do innych artykułów.
 *
 * @param articleTitle Tytuł artykułu do przeskanowania.
 * @return Zbiór (Set) tytułów artykułów znalezionych na stronie.
 */
private fun extractArticleLinks(articleTitle: String): Set<String> {
    val foundLinks = mutableSetOf<String>()
    try {
        val url = buildWikipediaUrl(articleTitle)
        val doc = Jsoup.connect(url).get()

        // Selektor CSS:
        // #mw-content-text -> wybierz główny kontener z treścią artykułu
        // a[href] -> wewnątrz niego znajdź wszystkie znaczniki <a>, które mają atrybut href
        val links = doc.select("#mw-content-text a[href]")

        for (link in links) {
            val href = link.attr("href")

            // Filtrujemy linki, które nas interesują:
            // 1. Muszą zaczynać się od "/wiki/" - to standard dla linków do artykułów.
            // 2. Nie mogą zawierać ":" - to eliminuje strony specjalne (np. "Pomoc:", "Plik:", "Wikipedia:").
            // 3. Nie mogą być linkiem do strony głównej.
            if (href.startsWith("/wiki/") && !href.contains(":") && href != "/wiki/Strona_główna") {
                // Wyciągamy sam tytuł z linku (usuwamy "/wiki/")
                val title = href.substringAfter("/wiki/")
                // Dekodujemy tytuł, aby zamienić np. %C4%85 na "ą"
                val decodedTitle = URLDecoder.decode(title, "UTF-8")
                foundLinks.add(decodedTitle)
            }
        }
    } catch (e: HttpStatusException) {
        // Ignorujemy błędy typu 404 (strona nie znaleziona)
        println("! Nie można odnaleźć strony dla: $articleTitle (błąd ${e.statusCode})")
    } catch (e: Exception) {
        // Inne błędy (np. brak połączenia)
        println("! Wystąpił błąd podczas przetwarzania $articleTitle: ${e.message}")
    }
    return foundLinks
}

/**
 * Główna funkcja scrapująca. Realizuje algorytm BFS do znalezienia wszystkich
 * artykułów w zasięgu `maxDepth` od artykułu `startTerm`.
 *
 * @param startTerm Tytuł artykułu startowego.
 * @param maxDepth Maksymalna głębokość przeszukiwania.
 * @return Mapa, gdzie kluczem jest poziom głębokości, a wartością zbiór tytułów artykułów.
 */
fun findWikipediaLinks(startTerm: String, maxDepth: Int): Map<Int, Set<String>> {
    // Mapa do przechowywania wyników dla każdego poziomu
    val results = mutableMapOf<Int, MutableSet<String>>()
    // Kolejka do zarządzania artykułami do odwiedzenia (rdzeń BFS)
    // Przechowujemy parę: (Tytuł Artykułu, Obecna Głębokość)
    val queue = ArrayDeque<Pair<String, Int>>()
    // Zbiór do śledzenia już odwiedzonych artykułów, aby unikać pętli i powtórzeń
    val visited = mutableSetOf<String>()

    // Inicjalizacja algorytmu
    queue.add(startTerm to 0)
    visited.add(startTerm)

    println("Rozpoczynam przeszukiwanie...")

    // Pętla działa, dopóki w kolejce są jeszcze jakieś artykuły do przetworzenia
    while (queue.isNotEmpty()) {
        val (currentTitle, currentDepth) = queue.removeFirst()

        println("Przetwarzam: $currentTitle (głębokość: $currentDepth)")

        // Jeśli osiągnęliśmy maksymalną głębokość, nie szukamy już dalej z tego miejsca
        if (currentDepth >= maxDepth) {
            continue
        }

        // Znajdź wszystkie linki na bieżącej stronie
        val newLinks = extractArticleLinks(currentTitle)

        for (linkTitle in newLinks) {
            // Jeśli jeszcze nie odwiedzaliśmy tego linku...
            if (linkTitle !in visited) {
                // ...dodaj go do już odwiedzonych
                visited.add(linkTitle)
                // Dodaj go do kolejki do przetworzenia w przyszłości
                val nextDepth = currentDepth + 1
                queue.add(linkTitle to nextDepth)
                // Zapisz wynik na odpowiednim poziomie głębokości
                results.getOrPut(nextDepth) { mutableSetOf() }.add(linkTitle)
            }
        }
    }
    println("Zakończono przeszukiwanie.")
    return results
}