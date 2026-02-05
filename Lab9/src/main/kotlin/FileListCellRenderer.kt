package org.example

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Component
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.filechooser.FileSystemView

/**
 * Renderer listy plików obsługujący systemowe ikony plików oraz SVG dla folderów.
 */
class FileListCellRenderer : DefaultListCellRenderer() {
    private val fileSystemView = FileSystemView.getFileSystemView()
    
    // Teraz ładujemy ikonę jako zasób z classpath (folder.svg)
    private val folderIcon = try {
        FlatSVGIcon("folder.svg", 20, 20)
    } catch (e: Exception) {
        null
    }

    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        val file = value as? File
        
        if (file != null) {
            label.text = file.name
            try {
                if (file.isDirectory && folderIcon != null) {
                    label.icon = folderIcon
                } else {
                    label.icon = fileSystemView.getSystemIcon(file)
                }
            } catch (e: Exception) {
                // błąd ikony
            }
        }
        
        return label
    }
}