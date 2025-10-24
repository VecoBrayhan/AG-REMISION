package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Un Composable que espera una implementación de plataforma para renderizar
 * y mostrar las páginas de un archivo PDF.
 *
 * @param modifier El modificador para aplicar al contenedor del visor.
 * @param pdfBytes El contenido del archivo PDF como un array de bytes.
 */
@Composable
expect fun PdfViewer(
    modifier: Modifier = Modifier,
    pdfBytes: ByteArray
)