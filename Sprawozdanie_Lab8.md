![[logo_wydzial.png]]

| **Imię i Nazwisko**     | **Nr Albumu** | **Przedmiot**                       | **Semestr** |
| ----------------------- | ------------- | ----------------------------------- | ----------- |
| **Damian Skonieczny**   | 122421        | Programowanie Współbieżne           | V           |
| **Prowadzący**          | **Typ zajęć** | **Temat**                           |             |
| **dr Damian Ledziński** | Laboratorium  | Wielowątkowość w aplikacjach GUI – Kalkulator Silni |             |

# 1. Opis tematu i wprowadzenie

Tematem ósmego laboratorium było stworzenie aplikacji okienkowej (Swing), która wykonuje długotrwałe obliczenia bez "mrożenia" interfejsu użytkownika. Jako przykład obliczeń wybraliśmy silnię z dużych liczb, co przy użyciu `BigInteger` potrafi zająć procesorowi chwilę czasu.

Głównym problemem w aplikacjach GUI (niezależnie czy to Swing, JavaFX czy Android) jest to, że interfejs działa na jednym wątku (tzw. Event Dispatch Thread - EDT). Jeśli wrzucimy na ten wątek ciężkie obliczenia, okno przestaje reagować na kliknięcia, nie przesuwa się i wygląda na zawieszone. Rozwiązaniem jest delegowanie pracy do osobnego wątku roboczego i bezpieczne aktualizowanie paska postępu z powrotem na wątku EDT.

W tym zadaniu Kotlin znowu pokazał swoją wyższość nad Javą – funkcja `thread { ... }` zamiast tworzenia nowej klasy anonimowej czy skomplikowanych Executorów sprawia, że kod czyta się jak dobrą książkę.

# 2. Kody
### Plik: Main.kt

Jest to główny i jedyny plik aplikacji, w którym zawarta jest cała logika okna oraz algorytm obliczania silni z aktualizacją paska postępu.

```kotlin
package org.example

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.math.BigInteger
import javax.swing.*
import kotlin.concurrent.thread

/**
 * Aplikacja okienkowa do obliczania silni z paskiem postępu.
 * Wykorzystuje Swing i oddzielne wątki do obliczeń, aby nie blokować UI.
 * 
 * @author Damian Skonieczny
 */
class FactorialApp : JFrame("Kalkulator Silni") {
    private val inputField = JTextField(10)
    private val calculateButton = JButton("Oblicz")
    private val progressBar = JProgressBar(0, 100)
    private val resultArea = JTextArea(10, 30)

    init {
        setupUI()
        setupListeners()
    }

    /**
     * Konfiguracja wyglądu interfejsu użytkownika.
     */
    private fun setupUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout(10, 10)

        val topPanel = JPanel(FlowLayout())
        topPanel.add(JLabel("Podaj liczbę:"))
        topPanel.add(inputField)
        topPanel.add(calculateButton)

        resultArea.isEditable = false
        val scrollPane = JScrollPane(resultArea)

        progressBar.isStringPainted = true
        progressBar.preferredSize = Dimension(300, 25)

        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        bottomPanel.add(progressBar, BorderLayout.NORTH)

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(null)
    }

    /**
     * Ustawienie słuchacza zdarzeń dla przycisku.
     */
    private fun setupListeners() {
        calculateButton.addActionListener {
            val n = inputField.text.toIntOrNull()
            if (n == null || n < 0) {
                JOptionPane.showMessageDialog(this, "Podaj poprawną liczbę nieujemną!")
            } else {
                startCalculation(n)
            }
        }
    }

    /**
     * Uruchamia obliczenia w osobnym wątku.
     * @param n Liczba, której silnię chcemy obliczyć.
     */
    private fun startCalculation(n: Int) {
        // Blokujemy przycisk na czas obliczeń
        calculateButton.isEnabled = false
        resultArea.text = "Obliczanie..."
        progressBar.value = 0

        // KLUCZOWY MOMENT: Odpalenie wątku roboczego
        thread {
            try {
                var result = BigInteger.ONE
                
                if (n > 0) {
                    for (i in 1..n) {
                        result = result.multiply(BigInteger.valueOf(i.toLong()))
                        
                        // Obliczamy postęp
                        val progress = (i.toDouble() / n * 100).toInt()
                        
                        // Sztuczne opóźnienie, aby postęp był widoczny dla małych liczb
                        // (procesor liczy to zbyt szybko dla małych n)
                        Thread.sleep(if (n < 100) 50 else 5)

                        // AKTUALIZACJA UI: Musi wrócić na wątek Swing (EDT)
                        SwingUtilities.invokeLater {
                            progressBar.value = progress
                        }
                    }
                } else {
                    SwingUtilities.invokeLater { progressBar.value = 100 }
                }

                // Po zakończeniu wyświetlamy wynik na wątku UI
                val finalResult = result.toString()
                SwingUtilities.invokeLater {
                    resultArea.text = "Wynik silni dla $n:
$finalResult"
                    calculateButton.isEnabled = true
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    resultArea.text = "Błąd: ${e.message}"
                    calculateButton.isEnabled = true
                }
            }
        }
    }
}

/**
 * Główna funkcja uruchamiająca aplikację okienkową.
 */
fun main() {
    // Ustawiamy wygląd systemu operacyjnego (ładniejszy button itp.)
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    
    SwingUtilities.invokeLater {
        val app = FactorialApp()
        app.isVisible = true
    }
}
```

# 3. Podsumowanie

Aplikacja poprawnie oblicza silnię nawet dla bardzo dużych liczb (np. 1000), co generuje wynik o setkach cyfr. Dzięki zastosowaniu klasy `BigInteger` nie mamy problemu z przepełnieniem zakresu.

Poniżej zrzut ekranu działającej aplikacji podczas obliczeń:

(Tutaj wstaw zdjęcie: ![Screenshot_Aplikacji](screenshot_lab8.png))

Najważniejszym elementem podsumowania jest fakt, że podczas gdy pasek postępu "idzie do przodu", użytkownik nadal może np. przesunąć okno lub zminimalizować je – co świadczy o poprawnym odseparowaniu logiki obliczeń od wątku interfejsu graficznego.

# 4. Wnioski

1.  **Swing nie jest "thread-safe"**: Wszystkie zmiany w wyglądzie okna (jak zmiana wartości paska postępu) muszą odbywać się wewnątrz `SwingUtilities.invokeLater`. Próba zmiany UI z innego wątku często kończy się dziwnymi błędami.
2.  **User Experience (UX)**: Pasek postępu to nie tylko bajer. W aplikacjach współbieżnych daje on użytkownikowi znać, że program żyje i pracuje, a nie po prostu się zawiesił.
3.  **Kotlin vs Java**: Kolejny raz Kotlin wygrywa prostotą. Funkcja `thread { ... }` jest genialna w swojej prostocie. W Javie musiałbym pisać `new Thread(new Runnable() { ... }).start()`, co tylko zaciemnia obraz tego, co program faktycznie robi.
4.  **BigInteger**: Obliczanie silni uświadamia, jak szybko liczby mogą rosnąć i dlaczego standardowe typy jak `Long` (które w Kotlinie są super proste w użyciu) czasem po prostu nie wystarczają.
