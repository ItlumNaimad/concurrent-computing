package org.example

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    // val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Witaj użytkowniku. Podaj hasło, do przeszukiwań")

    val termin = readln()

    println("A teraz proszę o podanie głębokości przeszukiwania (np. 2)")

    var depth: Int? = null
    while(depth == null){
        depth = readln().toIntOrNull()
        if(depth == null){
            println("To nie jest poprawna liczba. Spróbuj ponownie.")
        }
    }

    println("Rozpoczynam szukanie dla '$termin' z głębokością $depth...")
    scraper(termin)
}