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
// GUI: Klasa dziedziczy po JFrame, co oznacza, że jest to główne okno aplikacji.
class FactorialApp : JFrame("Kalkulator Silni") {
    // GUI: Definicja komponentów interfejsu użytkownika
    private val inputField = JTextField(10) // Pole tekstowe
    private val calculateButton = JButton("Oblicz") // Przycisk
    private val progressBar = JProgressBar(0, 100) // Pasek postępu
    private val resultArea = JTextArea(10, 30) // Obszar tekstowy na wynik

    init {
        setupUI()
        setupListeners()
    }

    /**
     * Konfiguracja wyglądu interfejsu użytkownika.
     */
    private fun setupUI() {
        // GUI: Ustawienie operacji zamknięcia okna (zakończenie aplikacji)
        defaultCloseOperation = EXIT_ON_CLOSE
        // GUI: Ustawienie menedżera układu BorderLayout (Północ, Południe, Wschód, Zachód, Centrum)
        layout = BorderLayout(10, 10)

        // GUI: Tworzenie panelu górnego z układem FlowLayout (elementy jeden obok drugiego)
        val topPanel = JPanel(FlowLayout())
        topPanel.add(JLabel("Podaj liczbę:"))
        topPanel.add(inputField)
        topPanel.add(calculateButton)

        resultArea.isEditable = false
        // GUI: Dodanie paska przewijania do obszaru tekstowego
        val scrollPane = JScrollPane(resultArea)

        progressBar.isStringPainted = true
        progressBar.preferredSize = Dimension(300, 25)

        // GUI: Tworzenie panelu dolnego dla paska postępu
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        bottomPanel.add(progressBar, BorderLayout.NORTH)

        // GUI: Dodawanie paneli do głównego okna w odpowiednich strefach BorderLayout
        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        // GUI: Dopasowanie rozmiaru okna do zawartości
        pack()
        // GUI: Wyśrodkowanie okna na ekranie
        setLocationRelativeTo(null)
    }

    /**
     * Ustawienie słuchacza zdarzeń dla przycisku.
     */
    private fun setupListeners() {
        // GUI: Rejestracja obsługi zdarzenia kliknięcia przycisku
        calculateButton.addActionListener {
            val n = inputField.text.toIntOrNull()
            if (n == null || n < 0) {
                // GUI: Wyświetlenie okna dialogowego z komunikatem
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
        // GUI: Blokujemy przycisk na czas obliczeń, aby uniknąć wielokrotnego uruchomienia
        calculateButton.isEnabled = false
        resultArea.text = "Obliczanie..."
        progressBar.value = 0

        // WĄTKOWANIE: Uruchomienie nowego wątku, aby nie blokować głównego wątku interfejsu (EDT).
        // Dzięki temu aplikacja pozostaje responsywna podczas długich obliczeń.
        thread {
            try {
                var result = BigInteger.ONE
                
                if (n > 0) {
                    for (i in 1..n) {
                        result = result.multiply(BigInteger.valueOf(i.toLong()))
                        
                        // Obliczamy postęp
                        val progress = (i.toDouble() / n * 100).toInt()
                        
                        // WĄTKOWANIE: Symulacja długotrwałej operacji (sztuczne opóźnienie)
                        Thread.sleep(if (n < 100) 50 else 5)

                        // WĄTKOWANIE: Aktualizacja UI z wątku pobocznego.
                        // Swing nie jest bezpieczny wątkowo, więc zmiany w UI muszą być zlecane
                        // do wątku dystrybucji zdarzeń (EDT) za pomocą SwingUtilities.invokeLater.
                        SwingUtilities.invokeLater {
                            progressBar.value = progress
                        }
                    }
                } else {
                    SwingUtilities.invokeLater { progressBar.value = 100 }
                }

                // Po zakończeniu wyświetlamy wynik
                val finalResult = result.toString()
                // WĄTKOWANIE: Ponowne użycie invokeLater do bezpiecznej aktualizacji UI po zakończeniu obliczeń
                SwingUtilities.invokeLater {
                    resultArea.text = "Wynik silni dla $n:\n$finalResult"
                    calculateButton.isEnabled = true
                }
            } catch (e: Exception) {
                // WĄTKOWANIE: Obsługa błędów również musi aktualizować UI w wątku EDT
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
    // GUI: Ustawiamy wygląd systemu operacyjnego (Look and Feel)
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    
    // WĄTKOWANIE: Uruchomienie interfejsu graficznego w wątku dystrybucji zdarzeń (EDT).
    // Jest to wymagane przez specyfikację Swing dla bezpieczeństwa wątkowego.
    SwingUtilities.invokeLater {
        val app = FactorialApp()
        app.isVisible = true
    }
}
