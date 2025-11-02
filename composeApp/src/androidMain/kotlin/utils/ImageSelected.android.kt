package utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

/**
 * Implementación 'actual' para Android del selector de imágenes.
 */
@Composable
actual fun rememberImagePicker(
    onImageSelected: (FileData?) -> Unit
): (ImageSource) -> Unit {
    val context = LocalContext.current

    // --- Launcher para CÁMARA ---
    // Usamos TakePicturePreview para obtener un Bitmap directamente, es más simple
    // y no requiere FileProvider ni permisos de escritura.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                // Convertir Bitmap a ByteArray
                val (bytes, fileName) = bitmapToFileData(bitmap, "photo.jpg")
                onImageSelected(FileData(bytes, fileName))
            } else {
                onImageSelected(null) // Usuario canceló
            }
        }
    )

    // --- Launcher para GALERÍA ---
    // Usamos el selector de fotos moderno de Android
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Convertir Uri a ByteArray
                val (bytes, fileName) = uriToFileData(uri, context, "image.jpg")
                onImageSelected(FileData(bytes, fileName))
            } else {
                onImageSelected(null) // Usuario canceló
            }
        }
    )

    // Retorna la función que decide qué launcher ejecutar
    return remember(cameraLauncher, galleryLauncher, onImageSelected) {
        { source: ImageSource ->
            when (source) {
                ImageSource.CAMERA -> {
                    // Nota: El permiso de CÁMARA se debe pedir por separado
                    cameraLauncher.launch(null) // No se necesita Uri con TakePicturePreview
                }
                ImageSource.GALLERY -> {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        }
    }
}

// --- Funciones de Ayuda ---

private fun bitmapToFileData(bitmap: Bitmap, defaultName: String): Pair<ByteArray, String> {
    val outputStream = ByteArrayOutputStream()
    // Comprime la imagen a JPEG. Ajusta la calidad (ej. 80) si es necesario.
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val bytes = outputStream.toByteArray()
    // Genera un nombre de archivo único
    val fileName = "photo_${System.currentTimeMillis()}.jpg"
    return bytes to fileName
}

private fun uriToFileData(uri: Uri, context: Context, defaultName: String): Pair<ByteArray, String> {
    val bytes = context.contentResolver.openInputStream(uri)?.use {
        it.readBytes()
    } ?: throw IllegalStateException("No se pudo leer el archivo desde la Uri: $uri")

    // Intenta obtener el nombre real del archivo, si no, usa uno genérico
    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) cursor.getString(nameIndex) else null
        } else {
            null
        }
    } ?: "image_${System.currentTimeMillis()}.${
        context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
    }"

    return bytes to fileName
}
