package org.example

/**
 * Główna funkcja programu. Interfejs użytkownika.
 * Oparte na Pararell Stream
 * @author Damian Skonieczny
 *
 * @see findWikipediaLinks
 * @see printResults
 */

fun main() {
    println("Witaj użytkowniku. Podaj hasło do przeszukiwania (np. Fortepian)")
    val termin = readln()

    println("A teraz proszę o podanie głębokości przeszukiwania (np. 2)")
    var depth: Int? = null
    while (depth == null || depth <= 0) {
        depth = readln().toIntOrNull()
        if (depth == null || depth <= 0) {
            println("To nie jest poprawna liczba dodatnia. Spróbuj ponownie.")
        }
    }

    println("\nRozpoczynam szukanie dla '$termin' z głębokością $depth...")

    // Wywołaj główną funkcję scrapującą
    val results = findWikipediaLinks(termin, depth)

    // Wydrukuj wyniki za pomocą funkcji z Interface.kt
    printResults(results)
}