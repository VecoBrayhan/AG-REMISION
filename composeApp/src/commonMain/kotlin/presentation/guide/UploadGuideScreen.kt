package presentation.guide

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image // <-- NECESARIO
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
import androidx.compose.ui.graphics.ImageBitmap // <-- NECESARIO
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale // <-- NECESARIO
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
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
// --- NUEVOS IMPORTS ---
import utils.ImageSource // <-- Importar ImageSource
import utils.rememberImagePicker // <-- Importar el nuevo ImagePicker
import utils.toKmpImageBitmap // <-- IMPORTAR LA FUNCIÓN EXPECT/ACTUAL

object UploadGuideScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val snackbarController = rememberSnackbarController()
        var showFilePicker by remember { mutableStateOf(false) }
        var selectedFile by remember { mutableStateOf<FileData?>(null) }

        // --- 1. Inicializa el ImagePicker ---
        val imagePickerLauncher = rememberImagePicker { fileData ->
            // El callback cuando se selecciona una imagen (cámara o galería)
            selectedFile = fileData
        }

        // --- 2. Actualiza el FilePicker para incluir todos los tipos ---
        FilePicker(
            show = showFilePicker,
            // Añade los tipos de imagen aquí también
            fileExtensions = listOf("pdf", "xls", "xlsx", "png", "jpg", "jpeg"),
            onFileSelected = { fileData ->
                selectedFile = fileData
                showFilePicker = false
            }
        )

        // --- 3. Actualiza el fileType para detectar imágenes ---
        val fileType = remember(selectedFile) {
            when {
                selectedFile?.fileName?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
                selectedFile?.fileName?.endsWith(".xls", ignoreCase = true) == true -> "excel"
                selectedFile?.fileName?.endsWith(".xlsx", ignoreCase = true) == true -> "excel"
                // Detectar imágenes
                selectedFile?.fileName?.endsWith(".png", ignoreCase = true) == true -> "image"
                selectedFile?.fileName?.endsWith(".jpg", ignoreCase = true) == true -> "image"
                selectedFile?.fileName?.endsWith(".jpeg", ignoreCase = true) == true -> "image"
                else -> "other"
            }
        }

        val scope = rememberCoroutineScope()
        val guideRepository = remember { FirestoreGuideRepository() }
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
                    // --- 4. Pasa los nuevos launchers a la vista inicial ---
                    InitialUploadView(
                        onSelectFileClick = { showFilePicker = true },
                        onCameraClick = { imagePickerLauncher(ImageSource.CAMERA) },
                        onGalleryClick = { imagePickerLauncher(ImageSource.GALLERY) }
                    )
                } else {
                    // Header con información del archivo (actualizado para 'image')
                    ProfessionalFilePreviewHeader(
                        fileName = selectedFile!!.fileName,
                        fileSize = selectedFile!!.bytes.size,
                        fileType = fileType, // Pasa el tipo de archivo actualizado
                        onChangeFileClick = {
                            selectedFile = null
                            extractedGuide = null
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // Vista previa del documento (actualizada para 'image')
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
                                    // --- 5. Añade la vista previa de imagen ---
                                    "image" -> {
                                        KmpImagePreview(
                                            bytes = selectedFile!!.bytes,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        PreviewNotAvailable()
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                }

                // --- 6. El botón de carga (NO CAMBIA NADA) ---
                // Tu lógica de IA ya es multimodal, así que no necesita cambios.
                LoadingActionButtonComponent(
                    text = "Extraer y guardar datos",
                    icon = Icons.Filled.SaveAlt,
                    isEnabled = selectedFile != null, // El botón maneja internamente !isLoading
                    isLoading = isLoading,
                    onClick = {
                        selectedFile?.let { file ->
                            scope.launch {
                                isLoading = true
                                extractedGuide = null
                                // Esta función ya envía el tipo de archivo correcto
                                // y el backend sabe qué hacer (texto o imagen)
                                val result = guideRepository.extractDataFromGuide(file.bytes, file.fileName)
                                if (result.isSuccess) {
                                    extractedGuide = result.getOrNull()
                                    snackbarController.showSuccess("Datos extraídos correctamente")

                                    if (extractedGuide != null) {
                                        val saveResult = guideRepository.addGuide(extractedGuide!!)
                                        if (saveResult.isSuccess) {
                                            snackbarController.showSuccess("¡Guía guardada exitosamente!")
                                            kotlinx.coroutines.delay(1500) // Espera para que el usuario vea el snackbar
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

/**
 * Vista previa de imagen KMP que decodifica un ByteArray.
 * (Funciona en Android y Desktop. iOS puede requerir una implementación 'actual')
 */
@Composable
private fun KmpImagePreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    // --- USA LA NUEVA FUNCIÓN 'EXPECT' ---
    val bitmap: ImageBitmap? = bytes.toKmpImageBitmap()
    // ------------------------------------

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Vista previa de imagen",
            modifier = modifier.padding(8.dp), // Añade un padding
            contentScale = ContentScale.Fit // Asegura que la imagen se ajuste
        )
    } else {
        PreviewNotAvailable(text = "No se pudo cargar la vista previa")
    }
}

// --- 7. Actualiza la vista inicial para mostrar los 3 botones ---
@Composable
private fun ColumnScope.InitialUploadView(
    onSelectFileClick: () -> Unit,
    onCameraClick: () -> Unit,
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
                    "Toma una foto, selecciona una imagen o un archivo (PDF/Excel) para extraer y guardar los datos.", // Texto actualizado
                    fontSize = 13.sp,
                    color = AppColors.VetTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Zona de carga
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

                // --- Botones de Cámara y Galería ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Botón de Cámara
                    Button(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.VetPrimaryColor
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cámara")
                    }

                    // Botón de Galería
                    Button(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.VetSecondaryColor
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Galería")
                    }
                }

                // --- Divisor "O" ---
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
                        "PDF / Excel / Img", // <-- Texto actualizado
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Formatos Soportados (Actualizado) ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileTypeChip("PDF", Icons.Outlined.PictureAsPdf)
                    FileTypeChip("XLS/XLSX", Icons.Outlined.TableChart)
                    FileTypeChip("PNG/JPG", Icons.Outlined.Image) // Añadido
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

// --- 8. Actualiza ProfessionalFilePreviewHeader (¡ERROR CORREGIDO!) ---
@Composable
private fun ProfessionalFilePreviewHeader(
    fileName: String,
    fileSize: Int,
    fileType: String,
    onChangeFileClick: () -> Unit
) {
    // --- Define los colores e iconos basados en el fileType ---

    // --- CORRECCIÓN ---
    // El error 'Not enough information' se debe a que el compilador no puede
    // inferir el tipo de la variable desestructurada (val (iconColor, ...))
    // antes de que se evalúe el 'when'.
    // Solución: Ser explícitos con los tipos.
    val fileColors: Triple<Color, Color, ImageVector> = when (fileType) {
        "pdf" -> Triple(Color(0xFFE53935), Color(0xFFE53935).copy(alpha = 0.15f), Icons.Outlined.PictureAsPdf)
        "excel" -> Triple(Color(0xFF43A047), Color(0xFF43A047).copy(alpha = 0.15f), Icons.Outlined.TableChart)
        "image" -> Triple(Color(0xFF1E88E5), Color(0xFF1E88E5).copy(alpha = 0.15f), Icons.Outlined.Image)
        else -> Triple(Color.Gray, Color.Gray.copy(alpha = 0.15f), Icons.Outlined.Description)
    }

    val (iconColor, iconBgColor, fileIcon) = fileColors

    // --- FIN DE LA CORRECCIÓN ---

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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(iconBgColor), // Usa color dinámico
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fileIcon, // Usa icono dinámico
                    contentDescription = "File type",
                    tint = iconColor, // Usa color dinámico
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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

// Actualizado para tomar un texto opcional
@Composable
private fun PreviewNotAvailable(text: String = "Vista previa no disponible") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.HideImage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        Text(
            "El archivo se procesará correctamente",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

