# Dokumentacja Techniczna: Aplikacja Kalkulator Silni

## Wprowadzenie
Aplikacja jest programem okienkowym napisanym w języku Kotlin, wykorzystującym bibliotekę **Swing** do budowy interfejsu użytkownika. Głównym celem aplikacji jest obliczanie silni dla zadanej liczby całkowitej. Ze względu na potencjalnie długi czas obliczeń oraz potrzebę wizualizacji postępu, zastosowano mechanizmy wielowątkowości.

## 1. Budowanie Aplikacji Okienkowej (Swing)

### Struktura Klasy
Główna klasa `FactorialApp` dziedziczy po `JFrame`, co czyni ją głównym oknem aplikacji.
```kotlin
class FactorialApp : JFrame("Kalkulator Silni")
```

### Komponenty Interfejsu
Interfejs użytkownika składa się z następujących elementów:
*   **JTextField**: Pole tekstowe do wprowadzania liczby.
*   **JButton**: Przycisk "Oblicz" inicjujący proces.
*   **JProgressBar**: Pasek postępu wizualizujący stan obliczeń (0-100%).
*   **JTextArea**: Obszar tekstowy do wyświetlania wyniku (umieszczony w `JScrollPane` dla przewijania).

### Zarządzanie Układem (Layout Managers)
Aplikacja wykorzystuje zagnieżdżone menedżery układu do rozmieszczenia elementów:
*   **BorderLayout**: Główny układ okna. Dzieli przestrzeń na regiony (Północ, Południe, Centrum).
*   **FlowLayout**: Użyty w górnym panelu (`topPanel`) do ułożenia etykiety, pola tekstowego i przycisku w jednym rzędzie.

### Uruchamianie Aplikacji
Zgodnie z zasadami Swing, interfejs graficzny powinien być tworzony i modyfikowany wyłącznie w wątku dystrybucji zdarzeń (Event Dispatch Thread - EDT). W funkcji `main` realizowane jest to poprzez:
```kotlin
SwingUtilities.invokeLater {
    val app = FactorialApp()
    app.isVisible = true
}
```

## 2. Mechanizmy Wątkowania (Concurrency)

### Problem Blokowania UI
Wykonywanie długotrwałych operacji (takich jak obliczanie silni dla dużych liczb z opóźnieniami) bezpośrednio w wątku EDT spowodowałoby "zamrożenie" interfejsu. Użytkownik nie mógłby przesuwać okna ani klikać przycisków, a system operacyjny mógłby uznać aplikację za nieodpowiadającą.

### Rozwiązanie: Osobny Wątek Roboczy
Aby temu zapobiec, obliczenia zostały przeniesione do osobnego wątku przy użyciu funkcji pomocniczej z biblioteki standardowej Kotlina:
```kotlin
thread {
    // Kod wykonywany w tle
}
```

### Komunikacja z Wątkiem UI (EDT)
Swing nie jest bezpieczny wątkowo (not thread-safe). Oznacza to, że nie można modyfikować komponentów UI (np. ustawiać wartości paska postępu czy tekstu wyniku) bezpośrednio z wątku roboczego.

Aby bezpiecznie zaktualizować interfejs z wątku tła, użyto metody `SwingUtilities.invokeLater`. Metoda ta kolejkuje przekazane zadanie (Runnable) do wykonania w wątku EDT w najbliższym możliwym momencie.

Przykłady użycia w kodzie:
1.  **Aktualizacja paska postępu:**
    ```kotlin
    SwingUtilities.invokeLater {
        progressBar.value = progress
    }
    ```
2.  **Wyświetlenie wyniku i odblokowanie przycisku:**
    ```kotlin
    SwingUtilities.invokeLater {
        resultArea.text = "Wynik..."
        calculateButton.isEnabled = true
    }
    ```

### Symulacja Obciążenia
W pętli obliczeniowej zastosowano `Thread.sleep(...)`. Ma to na celu spowolnienie procesu dla małych liczb, aby użytkownik mógł zauważyć działanie paska postępu. W rzeczywistych zastosowaniach obliczeniowych zazwyczaj unika się sztucznych opóźnień, chyba że są one wymagane przez logikę biznesową.

## Podsumowanie
Aplikacja demonstruje klasyczny wzorzec projektowy dla aplikacji desktopowych:
1.  **UI Thread (EDT)**: Obsługuje zdarzenia (kliknięcia), rysuje interfejs.
2.  **Worker Thread**: Wykonuje ciężkie obliczenia w tle.
3.  **Synchronizacja**: Wątek roboczy zleca aktualizacje UI z powrotem do wątku EDT.
