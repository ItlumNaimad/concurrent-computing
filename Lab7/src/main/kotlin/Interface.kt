package org.example

/**
 * Funkcja pomocnicza do wyświetlania wyników przeszukiwania.
 * Formatuje wyniki w postaci drzewiastej struktury dla każdego poziomu głębokości.
 *
 * @param results Mapa zawierająca wyniki przeszukiwania, gdzie kluczem jest poziom głębokości.
 */
fun printResults(results: Map<Int, Set<String>>)
{
    if(results.isEmpty())
    {
        println("Linków nie znaleziono.")
        return
    }
    val sortedDepths = results.keys.sorted() // Sortujemy poziomy 1, 2, 3...

    for(depth in sortedDepths)
    {
        println("$depth. poziom:")
        val titles = results[depth]!!.toList() // Pobieramy listę tytułów dla danego poziomu
        titles.forEachIndexed { index, title ->
            val prefix = if(index == titles.lastIndex) "	└─" else "	├─"
            println("$prefix $title")
        }
    }
}
