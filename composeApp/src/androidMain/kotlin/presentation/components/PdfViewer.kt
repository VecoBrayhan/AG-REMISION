package presentation.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
actual fun PdfViewer(
    modifier: Modifier,
    pdfBytes: ByteArray
) {
    val renderer by remember(pdfBytes) {
        mutableStateOf<PdfRenderer?>(
            try {
                val tempFile = File.createTempFile("temp_pdf_", ".pdf")
                tempFile.writeBytes(pdfBytes)
                tempFile.deleteOnExit()
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        )
    }

    if (renderer == null) {
    } else {
        val pageCount = renderer?.pageCount ?: 0
        LazyColumn(modifier = modifier) {
            items(count = pageCount) { index ->
                val page = renderer?.openPage(index)
                val bitmap = Bitmap.createBitmap(page!!.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Página ${index + 1}",
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                renderer?.close()
            }
        }
    }
}