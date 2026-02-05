package org.example

/**
 * Główna funkcja programu. Interfejs użytkownika.
 * Oparte na wzorcu Producent-Konsument z wykorzystaniem BlockingQueue.
 * @author Damian Skonieczny
 *
 * @see findWikipediaLinks
 * @see printResults
 */
fun main(args: Array<String>) {
    val termin = if (args.isNotEmpty()) args[0] else {
        println("Witaj użytkowniku. Podaj hasło do przeszukiwania (np. Fortepian)")
        readln().trim()
    }

    val depth = if (args.size >= 2) args[1].toIntOrNull() else {
        println("A teraz proszę o podanie głębokości przeszukiwania (np. 2)")
        var d: Int? = null
        while (d == null || d <= 0) {
            val depthInput = readln().trim()
            d = depthInput.toIntOrNull()
            if (d == null || d <= 0) {
                println("To nie jest poprawna liczba dodatnia. Spróbuj ponownie.")
            }
        }
        d
    }

    if (depth == null) return

    println("\nRozpoczynam szukanie dla '$termin' z głębokością $depth...")

    val startTime = System.currentTimeMillis()
    val results = findWikipediaLinks(termin, depth)
    val endTime = System.currentTimeMillis()

    // Wydrukuj wyniki za pomocą funkcji z Interface.kt
    printResults(results)

    val totalLinks = results.values.sumOf { it.size }
    println("\n--------------------------------")
    println("CZAS WYKONANIA ALGORYTMU: ${endTime - startTime} ms")
    println("ZNALEZIONO ŁĄCZNIE LINKÓW: $totalLinks")
    println("--------------------------------")
}
