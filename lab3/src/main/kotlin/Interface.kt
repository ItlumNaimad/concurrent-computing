package org.example

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
        println("\n$depth. poziom:")
        //results[depth]?.forEach { println(it) }
        val titles = results[depth]!!.toList() // Pobieramy listę tytułów dla danego poziomu
        titles.forEachIndexed { index, title ->
            val prefix = if(index == titles.lastIndex) "\t└─" else "\t├─"
            println("$prefix $title")
        }
    }
}