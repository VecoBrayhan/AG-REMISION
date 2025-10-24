package utils

import androidx.compose.runtime.Composable

/**
 * Contiene los datos de un archivo seleccionado.
 * @param bytes El contenido del archivo como un array de bytes.
 * @param fileName El nombre del archivo.
 */
data class FileData(val bytes: ByteArray, val fileName: String)

/**
 * Una función Composable que espera una implementación de plataforma para mostrar un selector de archivos.
 * @param show Controla si el selector de archivos debe mostrarse.
 * @param fileExtensions Lista de extensiones de archivo permitidas (ej: "pdf", "xlsx").
 * @param onFileSelected Callback que se ejecuta cuando un archivo es seleccionado o la selección es cancelada.
 */
@Composable
expect fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (FileData?) -> Unit
)