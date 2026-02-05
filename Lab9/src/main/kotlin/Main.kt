package org.example

import javax.swing.SwingUtilities

/**
 * Punkt wejścia do aplikacji Reaktywnego Eksploratora Plików.
 */
fun main() {
    // Inicjalizacja motywu FlatLaf
    ThemeManager.init()

    SwingUtilities.invokeLater {
        val app = ExplorerUI()
        app.isVisible = true
    }
}