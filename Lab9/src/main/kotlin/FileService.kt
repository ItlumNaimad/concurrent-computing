package org.example

import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.File

/**
 * Serwis obsługujący reaktywne operacje na plikach.
 */
class FileService {

    /**
     * Zwraca strumień plików z danego katalogu, przefiltrowany i posortowany.
     */
    fun listFiles(directory: File, filter: String = ""): Flux<File> {
        val filesArray = directory.listFiles() ?: arrayOf()
        
        return Flux.fromArray(filesArray)
            .subscribeOn(Schedulers.boundedElastic())
            .filter { file ->
                filter.isEmpty() || file.name.contains(filter, ignoreCase = true)
            }
            .sort { f1, f2 ->
                // Katalogi zawsze na górze, potem alfabetycznie
                when {
                    f1.isDirectory && !f2.isDirectory -> -1
                    !f1.isDirectory && f2.isDirectory -> 1
                    else -> f1.name.lowercase().compareTo(f2.name.lowercase())
                }
            }
    }
}
