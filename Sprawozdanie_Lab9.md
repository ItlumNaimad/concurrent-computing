![[logo_wydzial.png]]

| **Imię i Nazwisko**     | **Nr Albumu** | **Przedmiot**                       | **Semestr** |
| ----------------------- | ------------- | ----------------------------------- | ----------- |
| **Damian Skonieczny**   | 122421        | Programowanie Współbieżne           | V           |
| **Prowadzący**          | **Typ zajęć** | **Temat**                           |             |
| **dr Damian Ledziński** | Laboratorium  | Programowanie Reaktywne – Eksplorator Plików |             |

# 1. Opis tematu i wprowadzenie

Dziewiąte laboratorium to wejście na wyższy poziom programowania współbieżnego, czyli w świat **Reactive Streams**. Zamiast ręcznie zarządzać wątkami jak w poprzednich zadaniach, użyliśmy biblioteki **Project Reactor** (Flux i Mono), aby stworzyć reaktywny eksplorator plików.

Głównym wyzwaniem było stworzenie wyszukiwarki plików, która nie "zabija" procesora przy każdym naciśnięciu klawisza. Zastosowaliśmy tu mechanizm *Backpressure* oraz operatory takie jak `sampleTimeout` (debounce), które sprawiają, że przeszukiwanie dysku zaczyna się dopiero, gdy użytkownik przestanie pisać na ułamek sekundy. Dodatkowo, aby aplikacja nie wyglądała jak z lat 90., użyłem biblioteki **FlatLaf**, która nadaje Swingowi nowoczesny wygląd (tryb ciemny/jasny).

# 2. Kody
### Plik: Main.kt
Punkt wejścia, który inicjalizuje nowoczesny wygląd i odpala okno.
```kotlin
package org.example

import javax.swing.SwingUtilities

fun main() {
    ThemeManager.init() // Modern UI
    SwingUtilities.invokeLater {
        val app = ExplorerUI()
        app.isVisible = true
    }
}
```

### Plik: FileService.kt
Serce reaktywności. Zamiast zwracać zwykłą listę, zwraca `Flux<File>`, co pozwala na asynchroniczne "strumieniowanie" wyników z dysku.
```kotlin
package org.example

import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.File

class FileService {
    fun listFiles(directory: File, filter: String = ""): Flux<File> {
        val filesArray = directory.listFiles() ?: arrayOf()
        
        return Flux.fromArray(filesArray)
            .subscribeOn(Schedulers.boundedElastic()) // Ważne: I/O na dedykowanej puli
            .filter { file ->
                filter.isEmpty() || file.name.contains(filter, ignoreCase = true)
            }
            .sort { f1, f2 ->
                when {
                    f1.isDirectory && !f2.isDirectory -> -1
                    !f1.isDirectory && f2.isDirectory -> 1
                    else -> f1.name.lowercase().compareTo(f2.name.lowercase())
                }
            }
    }
}
```

### Plik: ExplorerUI.kt (Fragment logiki wyszukiwania)
Tutaj dzieje się magia reaktywna – obsługa zdarzeń z pola tekstowego.
```kotlin
    private val searchSink = Sinks.many().multicast().onBackpressureBuffer<String>()

    private fun setupReactiveLogic() {
        searchSink.asFlux()
            .sampleTimeout { Mono.delay(Duration.ofMillis(300)) } // Czekaj 300ms po pisaniu
            .distinctUntilChanged() // Szukaj tylko jeśli tekst faktycznie się zmienił
            .subscribeOn(Schedulers.parallel())
            .subscribe { searchText -> 
                refreshFiles(searchText) 
            }
    }
```

# 3. Podsumowanie

Aplikacja jest w pełni funkcjonalnym eksploratorem plików. Obsługuje nawigację po folderach (w górę/w dół), dynamiczne filtrowanie wyników w czasie rzeczywistym oraz zmianę motywu graficznego (Dark Mode / Light Mode). Dzięki zastosowaniu `Schedulers.boundedElastic()`, nawet przeglądanie bardzo dużych folderów systemowych nie powoduje najmniejszego przycięcia interfejsu użytkownika.

Poniżej zrzut ekranu przedstawiający nowoczesny interfejs w trybie ciemnym:

(Tutaj wstaw zdjęcie: ![Screenshot_Eksplorator](screenshot_lab9.png))

# 4. Wnioski

1.  **Potęga operatorów Flux**: Operatory takie jak `sampleTimeout` czy `distinctUntilChanged` niesamowicie upraszczają życie. W czystej Javie/Kotlinie musiałbym ręcznie zarządzać Timerami i stanem poprzedniego zapytania, co zajęłoby pewnie 100 linii kodu więcej.
2.  **Responsywność przede wszystkim**: Dzięki reaktywności, status bar (`statusLabel`) może na bieżąco informować o skanowaniu, podczas gdy wyniki pojawiają się w liście płynnie jeden po drugim.
3.  **FlatLaf to zbawienie dla Swinga**: Po tylu laboratoriach z "szarym" Swingiem, użycie FlatLaf pokazało, że aplikacje desktopowe w Javie/Kotlinie wcale nie muszą wyglądać staro. 
4.  **Nauka reaktywności**: Przyznam, że na początku zrozumienie różnicy między `Schedulers.parallel()` a `boundedElastic()` było trudne, ale teraz widzę, że to klucz do wydajnych aplikacji, które nie blokują się przy operacjach na plikach.
5.  **Sinks**: Mechanizm `Sinks` to świetny sposób na połączenie "starych" zdarzeń Swinga (CaretListener) z nowoczesnym światem reaktywnym. Wygląda to dużo lepiej niż ręczne przekazywanie list przez interfejsy.
