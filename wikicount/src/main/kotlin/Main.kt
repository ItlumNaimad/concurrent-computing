package org.example

/**
 * URLEncoder.encode zamienia spacje na '+' domyślnie, ale Wikipedia używa '_'.
 * Po zakodowaniu, możemy ręcznie zamienić '+' na '_', a jeśli trzeba też inne znaki.
 * Lepszym podejściem jest prosta zamiana spacji na podkreślnik, bo Wikipedia jest dość liberalna w tej kwestii.
 * @param: term :String
 * @return: url: String
*/
fun buildWikipediaUrl(term: String): String {
    val formattedTerm = term.replace(' ', '_')
    return "https://pl.wikipedia.org/wiki/$formattedTerm"
}
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * Główna funkcja programu. Interfejs użytkownika
 * @param: null
 * @return: null
 * @author: Damian Skonieczny
 */
fun main() {
    // val name = "Kotlin"
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    println("Witaj użytkowniku. Podaj hasło, do przeszukiwań")

    val termin = readln()

    println("A teraz proszę o podanie głębokości przeszukiwania (np. 2)")

    var depth: Int? = null
    while(depth == null){
        /* Odczytaj input z klawiatury, przypisz do zmiany @depth.
        * Konwertuj typ zmiany do Int lub nadaj Null
        */
        depth = readln().toIntOrNull()
        if(depth == null){
            println("To nie jest poprawna liczba. Spróbuj ponownie.")
        }
    }

    println("Rozpoczynam szukanie dla '$termin' z głębokością $depth...")
    titleScraper(buildWikipediaUrl(termin.lowercase()))
}