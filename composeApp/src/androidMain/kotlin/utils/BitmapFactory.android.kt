package utils

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Implementación 'actual' para Android.
 * Usa BitmapFactory para decodificar el ByteArray.
 */
@Composable
actual fun ByteArray.toKmpImageBitmap(): ImageBitmap? {
    return remember(this) { // 'remember' previene recálculos innecesarios
        try {
            BitmapFactory.decodeByteArray(this, 0, this.size)?.asImageBitmap()
        } catch (e: Exception) {
            println("Error al decodificar imagen en Android: ${e.message}")
            null
        }
    }
}
