package org.example

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


/**
 * Główna funkcja programu. Interfejs użytkownika
 * @author: Damian Skonieczny
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