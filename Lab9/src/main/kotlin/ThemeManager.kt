package org.example

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import java.awt.Color
import java.awt.Window
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Menedżer motywów wspierający tryb Jasny i Ciemny z niebieskimi akcentami.
 */
object ThemeManager {
    private var isDark = true

    fun init() {
        applyTheme()
    }

    fun toggleTheme() {
        isDark = !isDark
        applyTheme()
    }

    private fun applyTheme() {
        FlatAnimatedLafChange.showSnapshot()
        
        if (isDark) {
            FlatDarkLaf.setup()
        } else {
            FlatLightLaf.setup()
        }

        val blueAccent = Color(0x22C3E6)
        UIManager.put("Component.focusColor", blueAccent)
        UIManager.put("Button.focusedBorderColor", blueAccent)
        UIManager.put("Selection.background", blueAccent)
        UIManager.put("ProgressBar.foreground", blueAccent)

        FlatAnimatedLafChange.hideSnapshotWithAnimation()
        
        // Poprawione: Window.getWindows() zamiast SwingUtilities.getWindows()
        Window.getWindows().forEach { window -> 
            SwingUtilities.updateComponentTreeUI(window) 
        }
    }
    
    fun isDarkMode() = isDark
}