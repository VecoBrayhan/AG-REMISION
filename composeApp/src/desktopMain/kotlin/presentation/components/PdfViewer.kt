package presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
private sealed class PdfState {
    object Loading : PdfState()
    data class Success(val images: List<ImageBitmap>) : PdfState()
    data class Error(val message: String) : PdfState()
}

@Composable
actual fun PdfViewer(
    modifier: Modifier,
    pdfBytes: ByteArray
) {
    val pdfState by produceState<PdfState>(PdfState.Loading, pdfBytes) {
        value = withContext(Dispatchers.IO) {
            try {
                val document = PDDocument.load(pdfBytes)
                val renderer = PDFRenderer(document)
                val images = List(document.numberOfPages) { index ->
                    renderer.renderImageWithDPI(index, 150f).toComposeImageBitmap()
                }
                document.close()
                PdfState.Success(images)
            } catch (e: Exception) {
                e.printStackTrace()
                PdfState.Error("No se pudo cargar el PDF.")
            }
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val state = pdfState) {
            is PdfState.Loading -> {
                // Puedes poner un CircularProgressIndicator aquí si quieres
                Text("Cargando previsualización...")
            }
            is PdfState.Error -> {
                Text(state.message)
            }
            is PdfState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = state.images.size) { index ->
                        Image(
                            bitmap = state.images[index],
                            contentDescription = "Página ${index + 1}",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}