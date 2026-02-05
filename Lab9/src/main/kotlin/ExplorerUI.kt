package org.example

import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.awt.BorderLayout
import java.io.File
import java.time.Duration
import javax.swing.*
import javax.swing.border.EmptyBorder

class ExplorerUI : JFrame("Reaktywny Eksplorator") {
    private val fileService = FileService()
    private var currentDirectory = File(System.getProperty("user.home"))
    
    private val fileListModel = DefaultListModel<File>()
    private val fileList = JList(fileListModel)
    private val pathLabel = JLabel(currentDirectory.absolutePath)
    private val searchField = JTextField()
    private val statusLabel = JLabel("Gotowy")
    private val themeButton = JButton("🌓 Zmień motyw")
    
    private val searchSink = Sinks.many().multicast().onBackpressureBuffer<String>()

    init {
        setupUI()
        setupReactiveLogic()
        refreshFiles()
    }

    private fun setupUI() {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(900, 650)
        layout = BorderLayout()

        val topPanel = JPanel(BorderLayout(10, 10))
        topPanel.border = EmptyBorder(15, 15, 15, 15)

        val navBar = JPanel(BorderLayout(10, 0))
        val upButton = JButton("⬅ W górę").apply {
            addActionListener { goUp() }
        }
        
        themeButton.addActionListener { ThemeManager.toggleTheme() }
        
        navBar.add(upButton, BorderLayout.WEST)
        navBar.add(pathLabel, BorderLayout.CENTER)
        navBar.add(themeButton, BorderLayout.EAST)

        val searchBar = JPanel(BorderLayout(10, 0))
        searchBar.add(JLabel("Szukaj:"), BorderLayout.WEST)
        searchBar.add(searchField, BorderLayout.CENTER)
        
        searchField.addCaretListener { searchSink.tryEmitNext(searchField.text) }

        topPanel.add(navBar, BorderLayout.NORTH)
        topPanel.add(searchBar, BorderLayout.SOUTH)

        fileList.cellRenderer = FileListCellRenderer()
        fileList.fixedCellHeight = 30
        fileList.addListSelectionListener {
            if (!it.valueIsAdjusting && fileList.selectedValue != null) {
                val selected = fileList.selectedValue
                if (selected.isDirectory) changeDirectory(selected)
            }
        }

        val scrollPane = JScrollPane(fileList)
        scrollPane.border = BorderFactory.createCompoundBorder(
            EmptyBorder(0, 15, 0, 15),
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        )

        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = EmptyBorder(10, 15, 10, 15)
        bottomPanel.add(statusLabel, BorderLayout.WEST)

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        setLocationRelativeTo(null)
    }

    private fun setupReactiveLogic() {
        searchSink.asFlux()
            .sampleTimeout { Mono.delay(Duration.ofMillis(300)) }
            .distinctUntilChanged()
            .subscribeOn(Schedulers.parallel())
            .subscribe { searchText -> 
                refreshFiles(searchText) 
            }
    }

    private fun changeDirectory(dir: File) {
        currentDirectory = dir
        pathLabel.text = dir.absolutePath
        searchField.text = ""
        refreshFiles()
    }

    private fun goUp() {
        currentDirectory.parentFile?.let { changeDirectory(it) }
    }

    private fun refreshFiles(filter: String = "") {
        SwingUtilities.invokeLater { 
            fileListModel.clear()
            statusLabel.text = "Skanowanie..."
        }

        // Poprawione łańcuchowanie: doOnComplete musi być przed subscribe()
        fileService.listFiles(currentDirectory, filter)
            .doOnComplete {
                SwingUtilities.invokeLater { 
                    statusLabel.text = "Elementów: ${fileListModel.size()}" 
                }
            }
            .subscribe { file ->
                SwingUtilities.invokeLater { fileListModel.addElement(file) }
            }
    }
}