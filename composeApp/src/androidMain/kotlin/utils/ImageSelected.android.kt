package utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(
    onImageSelected: (FileData?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val (bytes, fileName) = uriToFileData(uri, context, "image.jpg")
                onImageSelected(FileData(bytes, fileName))
            } else {
                onImageSelected(null)
            }
        }
    )
    return remember(galleryLauncher, onImageSelected) {
        {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}
private fun uriToFileData(uri: Uri, context: Context, defaultName: String): Pair<ByteArray, String> {
    val bytes = context.contentResolver.openInputStream(uri)?.use {
        it.readBytes()
    } ?: throw IllegalStateException("No se pudo leer el archivo desde la Uri: $uri")
    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) cursor.getString(nameIndex) else null
        } else {
            null
        }
    } ?: "image_${System.currentTimeMillis()}.${
        context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
    }"

    return bytes to fileName
}