@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import data.FirebaseAuthRepository
import data.FirestoreGuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.launch
import presentation.components.ExcelViewer
import presentation.components.LoadingActionButtonComponent
import presentation.components.PdfViewer
import presentation.components.ReusableSnackbarHost
import presentation.components.TopBarComponent
import presentation.components.TopBarType
import presentation.components.rememberSnackbarController
import utils.AppColors
import utils.FileData
import utils.FilePicker
import utils.formatFileSize
import utils.rememberImagePicker
import utils.toKmpImageBitmap
object UploadGuideScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val snackbarController = rememberSnackbarController()
        var showFilePicker by remember { mutableStateOf(false) }
        var selectedFile by remember { mutableStateOf<FileData?>(null) }
        val imagePickerLauncher = rememberImagePicker { fileData ->
            selectedFile = fileData
        }
        FilePicker(
            show = showFilePicker,
            fileExtensions = listOf("pdf", "xls", "xlsx"),
            onFileSelected = { fileData ->
                selectedFile = fileData
                showFilePicker = false
            }
        )
        val fileType = remember(selectedFile) {
            when {
                selectedFile?.fileName?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
                selectedFile?.fileName?.endsWith(".xls", ignoreCase = true) == true -> "excel"
                selectedFile?.fileName?.endsWith(".xlsx", ignoreCase = true) == true -> "excel"
                selectedFile?.fileName?.endsWith(".png", ignoreCase = true) == true -> "image"
                selectedFile?.fileName?.endsWith(".jpg", ignoreCase = true) == true -> "image"
                selectedFile?.fileName?.endsWith(".jpeg", ignoreCase = true) == true -> "image"
                else -> "other"
            }
        }
        val scope = rememberCoroutineScope()
        val auth = remember { FirebaseAuthRepository() }
        val guideRepository = remember { FirestoreGuideRepository(auth) }
        var isLoading by remember { mutableStateOf(false) }
        var extractedGuide by remember { mutableStateOf<RemissionGuide?>(null) }
        Scaffold(
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) },
            topBar = {
                TopBarComponent(
                    type = TopBarType.MAIN,
                    title = "Cargar Guía de Remisión",
                    subtitle = "Procesamiento Automático",
                    icon = Icons.Filled.CloudUpload
                )
            },
            containerColor = AppColors.VetBackgroundLight
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
                    InitialUploadView(
                        onSelectFileClick = { showFilePicker = true },
                        onGalleryClick = { imagePickerLauncher() }
                    )
                } else {
                    ProfessionalFilePreviewHeader(
                        fileName = selectedFile!!.fileName,
                        fileSize = selectedFile!!.bytes.size,
                        fileType = fileType,
                        onChangeFileClick = {
                            selectedFile = null
                            extractedGuide = null
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                                    "image" -> {
                                        KmpImagePreview(
                                            bytes = selectedFile!!.bytes,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Text("Vista previa no disponible")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                LoadingActionButtonComponent(
                    text = "Extraer y guardar datos",
                    icon = Icons.Filled.SaveAlt,
                    isEnabled = selectedFile != null,
                    isLoading = isLoading,
                    onClick = {
                        selectedFile?.let { file ->
                            scope.launch {
                                isLoading = true
                                extractedGuide = null
                                val result = guideRepository.extractDataFromGuide(file.bytes, file.fileName)
                                if (result.isSuccess) {
                                    extractedGuide = result.getOrNull()
                                    snackbarController.showSuccess("Datos extraídos correctamente")
                                    if (extractedGuide != null) {
                                        val saveResult = guideRepository.addGuide(extractedGuide!!)
                                        if (saveResult.isSuccess) {
                                            snackbarController.showSuccess("¡Guía guardada exitosamente!")
                                            kotlinx.coroutines.delay(1500)
                                            selectedFile = null
                                            extractedGuide = null
                                        } else {
                                            snackbarController.showError("Error al guardar: ${saveResult.exceptionOrNull()?.message}")
                                        }
                                    } else {
                                        snackbarController.showError("Error: No se obtuvieron datos para guardar.")
                                    }
                                } else {
                                    snackbarController.showError(
                                        result.exceptionOrNull()?.message ?: "Error desconocido al procesar"
                                    )
                                }
                                isLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun KmpImagePreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = bytes.toKmpImageBitmap()

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Vista previa de imagen",
            modifier = modifier.padding(8.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Text("No se pudo cargar la vista.")
    }
}
@Composable
private fun ColumnScope.InitialUploadView(
    onSelectFileClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.VetPrimaryColor.copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                tint = AppColors.VetSecondaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¿Cómo funciona?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppColors.VetTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Selecciona una imagen de tu galería o un archivo (PDF/Excel) para extraer y guardar los datos.", // Texto actualizado
                    fontSize = 13.sp,
                    color = AppColors.VetTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    border = BorderStroke(2.dp, AppColors.VetSecondaryColor.copy(alpha = 0.3f)),
                    shape = MaterialTheme.shapes.extraLarge
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.VetPrimaryColor.copy(alpha = 0.15f),
                                    AppColors.VetSecondaryColor.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Upload",
                        modifier = Modifier.size(64.dp),
                        tint = AppColors.VetPrimaryColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Selecciona una fuente", // Texto actualizado
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.VetTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp), // <- Modificado
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.VetSecondaryColor
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Seleccionar de Galería") // <-- Texto actualizado
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(modifier = Modifier.weight(1f))
                    Text(
                        "O",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.VetTextSecondary
                    )
                    Divider(modifier = Modifier.weight(1f))
                }

                // --- Botón de Archivo (PDF/Excel) ---
                OutlinedButton(
                    onClick = onSelectFileClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.VetTextPrimary
                    ),
                    border = BorderStroke(1.dp, AppColors.VetAccentColor)
                ) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PDF / Excel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileTypeChip("PDF", Icons.Outlined.PictureAsPdf)
                    FileTypeChip("XLS/XLSX", Icons.Outlined.TableChart)
                    FileTypeChip("PNG/JPG", Icons.Outlined.Image)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun FileTypeChip(label: String, icon: ImageVector) {
    Surface(
        color = AppColors.VetSecondaryColor.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppColors.VetSecondaryColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.VetTextPrimary
            )
        }
    }
}

@Composable
private fun ProfessionalFilePreviewHeader(
    fileName: String,
    fileSize: Int,
    fileType: String,
    onChangeFileClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.VetTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatFileSize(fileSize)} • ${fileType.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.VetTextSecondary,
                    fontSize = 12.sp
                )
            }
            TextButton(
                onClick = onChangeFileClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.VetSecondaryColor
                )
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Cambiar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cambiar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
