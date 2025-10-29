@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.guide
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import presentation.components.ExcelViewer
import presentation.components.PdfViewer
import utils.FileData
import utils.FilePicker



// --- IMPORTACIONES CLAVE FALTANTES ---
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue  // Necesario para 'by'
import androidx.compose.runtime.setValue    // Necesario para 'by'
import kotlinx.coroutines.launch
import data.FirestoreGuideRepository
import domain.RemissionGuide
import presentation.components.rememberSnackbarController

object UploadGuideScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val snackbarController = rememberSnackbarController()

        var showFilePicker by remember { mutableStateOf(false) }
        var selectedFile by remember { mutableStateOf<FileData?>(null) }
        val fileType = remember(selectedFile) {
            when {
                selectedFile?.fileName?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
                selectedFile?.fileName?.endsWith(".xls", ignoreCase = true) == true -> "excel"
                selectedFile?.fileName?.endsWith(".xlsx", ignoreCase = true) == true -> "excel"
                else -> "other"
            }
        }
        FilePicker(
            show = showFilePicker,
            fileExtensions = listOf("pdf", "xls", "xlsx"),
            onFileSelected = { fileData ->
                selectedFile = fileData
                showFilePicker = false
            }
        )
        val scope = rememberCoroutineScope()
        val guideRepository = remember { FirestoreGuideRepository() }
        var isLoading by remember { mutableStateOf(false) }
        var extractedGuide by remember { mutableStateOf<RemissionGuide?>(null) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Carga de Guía") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedFile == null) {
                    InitialUploadView(onSelectFileClick = { showFilePicker = true })
                } else {
                    FilePreviewHeader(
                        fileName = selectedFile!!.fileName,
                        onChangeFileClick = { showFilePicker = true }
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        key(selectedFile!!.bytes) {
                            when (fileType) {
                                "pdf" -> {
                                    PdfViewer(
                                        modifier = Modifier.fillMaxSize(),
                                        pdfBytes = selectedFile!!.bytes
                                    )
                                }
                                "excel" -> {
                                    ExcelViewer(
                                        modifier = Modifier.fillMaxSize(),
                                        excelBytes = selectedFile!!.bytes
                                    )
                                }
                                else -> {
                                    Text("Previsualización no disponible para este tipo de archivo.")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Button(
                    onClick = {
                        selectedFile?.let { file ->
                            scope.launch {
                                isLoading = true
                                extractedGuide = null

                                val result = guideRepository.extractDataFromGuide(file.bytes, file.fileName)

                                if (result.isSuccess) {
                                    extractedGuide = result.getOrNull()
                                    // ¡Éxito! Ahora 'extractedGuide' tiene los datos
                                    // podrías mostrarlos en la UI
                                    snackbarController.showSuccess("Datos extraídos")
                                    // --- GUARDADO AUTOMÁTICO (Opcional) ---
                                    // O puedes guardarlo directamente en Firebase
                                    val saveResult = guideRepository.addGuide(extractedGuide!!)
                                    if(saveResult.isSuccess) {
                                        snackbarController.showSuccess("¡Guía guardada!")
                                        selectedFile = null // Limpiar para la próxima subida
                                    }

                                } else {
                                    // Usa el operador Elvis (?:) para dar un mensaje por defecto si es null
                                    snackbarController.showError(result.exceptionOrNull()?.message ?: "Ocurrió un error desconocido")
                                }
                                isLoading = false
                            }
                        }
                    },
                    enabled = selectedFile != null && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Extraer y Guardar Datos")
                    }
                }
            }
        }
    }
}
@Composable
private fun ColumnScope.InitialUploadView(onSelectFileClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Subir Guía de Remisión", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Selecciona un archivo PDF o Excel para iniciar el proceso.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Upload Icon",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Arrastra y suelta tu archivo", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onSelectFileClick) {
                Text("Seleccionar Archivo")
            }
        }
    }
    Spacer(modifier = Modifier.weight(1f))
}
@Composable
private fun FilePreviewHeader(fileName: String, onChangeFileClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Archivo seleccionado",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onChangeFileClick) {
            Text("Cambiar")
        }
    }
}