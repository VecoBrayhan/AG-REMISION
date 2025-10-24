package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (FileData?) -> Unit
) {
    LaunchedEffect(show) {
        if (show) {
            val fileDialog = FileDialog(null as? Frame, "Seleccionar Archivo", FileDialog.LOAD).apply {
                file = fileExtensions.joinToString(";") { "*.$it" } // Filtro para extensiones
                isVisible = true
            }

            val directory = fileDialog.directory
            val fileName = fileDialog.file

            if (directory != null && fileName != null) {
                val file = File(directory, fileName)
                onFileSelected(FileData(file.readBytes(), file.name))
            } else {
                onFileSelected(null)
            }
        }
    }
}