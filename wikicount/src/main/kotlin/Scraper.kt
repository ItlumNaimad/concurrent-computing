package org.example

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Wyodrębnia i drukuje zawartość znacznika <title> ze strony Wikipedii
 * odpowiadającą podanemu wyszukiwanemu hasłu.
 * @see <a href="https://jsoup.org/">
 * @param termin  wyszukiwania użyty do skonstruowania adresu URL strony Wikipedii
 */
fun titleScraper(termin: String) {
    /**
     * Funkcja JSoup typu Dokument odpowiedzialna za pobranie
     * kodu HTML strony
     * @see <a href="https://jsoup.org/cookbook/input/load-document-from-url">
    */
    val doc: Document = Jsoup.connect("https://pl.wikipedia.org/$termin").get()
    // Pobieranie zawartości znacznika <title>
    val title: String? = doc.title()
    println(title)
}