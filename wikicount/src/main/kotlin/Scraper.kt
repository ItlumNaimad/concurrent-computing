package org.example

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun scraper(termin: String) {
    val doc: Document = Jsoup.connect("https://pl.wikipedia.org/$termin").get()
    val title: String? = doc.title()
    println(title)
}