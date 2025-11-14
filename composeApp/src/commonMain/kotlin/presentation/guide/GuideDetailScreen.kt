@file:Suppress("UNRESOLVED_REFERENCE")
package presentation.guide
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import domain.RemissionGuide
import domain.model.Producto
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import data.FirebaseAuthRepository
import data.FirestoreGuideRepository
import domain.GuideStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import utils.AppColors

data class GuideDetailScreen(var initialGuide: RemissionGuide) : Screen {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarController = rememberSnackbarController()
        val auth = remember { FirebaseAuthRepository() }
        val guideRepository = remember { FirestoreGuideRepository(auth) }
        var guide by remember { mutableStateOf(initialGuide) }
        var isEditing by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showApproveDialog by remember { mutableStateOf(false) }
        val productsState = remember { mutableStateListOf<Producto>() }
        LaunchedEffect(initialGuide) {
            val initialProducts = initialGuide.extractedData["productosJson"]?.let { jsonString ->
                try {
                    jsonParser.decodeFromString<List<Producto>>(jsonString)
                } catch (e: SerializationException) {
                    println("Error deserializando productosJson: ${e.message}")
                    emptyList()
                } catch (e: Exception) {
                    println("Error inesperado deserializando productosJson: ${e.message}")
                    emptyList()
                }
            } ?: emptyList()
            productsState.clear()
            productsState.addAll(initialProducts)
        }

        Scaffold(
            topBar = {
                DetailTopBar(
                    onBackClick = { navigator.pop() },
                    guideName = guide.extractedData["nombreEmpresa"]
                        ?: guide.extractedData["laboratorio"]
                        ?: guide.guideName
                )
            },
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) },
            containerColor = AppColors.VetBackgroundLight
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HeaderCard(guide = guide)
                }
                item {
                    ProfessionalSectionCard(
                        title = "Información General",
                        icon = Icons.Outlined.Info
                    ) {
                        if (isEditing) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = guide.extractedData["nombreEmpresa"] ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(extractedData = guide.extractedData.toMutableMap().apply {
                                            this["nombreEmpresa"] = newValue
                                        })
                                    },
                                    label = { Text("Empresa/Origen") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = guide.ruc ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(ruc = newValue)
                                    },
                                    label = { Text("RUC") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = guide.date ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(date = newValue)
                                    },
                                    label = { Text("Fecha Documento") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = guide.extractedData["fechaRegistro"] ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(extractedData = guide.extractedData.toMutableMap().apply {
                                            this["fechaRegistro"] = newValue
                                        })
                                    },
                                    label = { Text("Fecha Registro") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false // Fecha de registro no debería ser editable
                                )
                                OutlinedTextField(
                                    value = guide.extractedData["fechaGuiaRemitente"] ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(extractedData = guide.extractedData.toMutableMap().apply {
                                            this["fechaGuiaRemitente"] = newValue
                                        })
                                    },
                                    label = { Text("Fecha Guía Remitente") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = guide.extractedData["costoTransporte"] ?: "",
                                    onValueChange = { newValue ->
                                        guide = guide.copy(extractedData = guide.extractedData.toMutableMap().apply {
                                            this["costoTransporte"] = newValue
                                        })
                                    },
                                    label = { Text("Costo Transporte") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.Business,
                                    label = "Empresa/Origen",
                                    value = guide.extractedData["nombreEmpresa"]
                                        ?: guide.extractedData["laboratorio"]
                                        ?: guide.guideName
                                )
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.Badge,
                                    label = "RUC",
                                    value = guide.ruc
                                )
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.CalendarMonth,
                                    label = "Fecha Documento",
                                    value = guide.date
                                )
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.Edit,
                                    label = "Fecha Registro",
                                    value = guide.extractedData["fechaRegistro"]
                                )
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.LocalShipping,
                                    label = "Fecha Guía Remitente",
                                    value = guide.extractedData["fechaGuiaRemitente"]
                                )
                                EnhancedInfoRow(
                                    icon = Icons.Outlined.AttachMoney,
                                    label = "Costo Transporte",
                                    value = guide.extractedData["costoTransporte"]
                                )
                                guide.status?.let { status ->
                                    if (status.isNotBlank() && status != "N/A") {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        StatusBadge(status = status)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ProfessionalSectionCard(
                        title = "Productos Recibidos",
                        icon = Icons.Outlined.Inventory2,
                        subtitle = if (!isEditing) "${productsState.size} producto${if (productsState.size != 1) "s" else ""}" else "Modo de Edición"
                    ) {
                        if (isEditing) {
                            Button(
                                onClick = {
                                    productsState.add(Producto(descripcion = "Nuevo Producto", cantidad = 1.0, unidad = "Und"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, "Añadir")
                                Spacer(Modifier.width(8.dp))
                                Text("Añadir Producto")
                            }
                        } else {
                            // Vista normal de solo lectura
                            if (productsState.isNotEmpty()) {
                                ProductTableHeader()
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                EmptyProductsView(
                                    message = guide.extractedData["productosError"]
                                        ?: "No se encontraron productos en esta guía."
                                )
                            }
                        }
                    }
                }
                items(productsState.size) { index ->
                    val product = productsState[index]
                    if (isEditing) {
                        EditableProductCard(
                            product = product,
                            onProductChange = { updatedProduct ->
                                productsState[index] = updatedProduct
                            },
                            onRemove = {
                                productsState.removeAt(index)
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        ProfessionalProductCard(product, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    if (isEditing) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    val updatedProductsJson = jsonParser.encodeToString(productsState.toList())
                                    val updatedExtractedData = guide.extractedData.toMutableMap().apply {
                                        this["productosJson"] = updatedProductsJson
                                    }
                                    guide = guide.copy(extractedData = updatedExtractedData)
                                    val result = guideRepository.addGuide(guide)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        initialGuide = guide
                                        isEditing = false
                                        snackbarController.showSuccess("Cambios guardados")
                                    } else {
                                        snackbarController.showError(result.exceptionOrNull()?.message ?: "Error al guardar")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.VetPrimaryColor)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Save, "Guardar")
                                Spacer(Modifier.width(8.dp))
                                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                guide = initialGuide
                                isEditing = false
                                val originalProducts = initialGuide.extractedData["productosJson"]?.let { jsonString ->
                                    try {
                                        jsonParser.decodeFromString<List<Producto>>(jsonString)
                                    } catch (e: Exception) { emptyList() }
                                } ?: emptyList()
                                productsState.clear()
                                productsState.addAll(originalProducts)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Filled.Cancel, "Cancelar")
                            Spacer(Modifier.width(8.dp))
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        if (guide.status == GuideStatus.PENDING_REVIEW) {
                            Button(
                                onClick = { isEditing = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.StatusApprovedColor) // Color azul
                            ) {
                                Icon(Icons.Filled.Edit, "Editar")
                                Spacer(Modifier.width(8.dp))
                                Text("Editar Guía", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showApproveDialog = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = AppColors.VetPrimaryColor) // Color verde
                            ) {
                                Icon(Icons.Filled.Check, "Aprobar")
                                Spacer(Modifier.width(8.dp))
                                Text("Aprobar y Enviar a SQL", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.StatusErrorColor),
                            border = BorderStroke(1.dp, AppColors.StatusErrorColor.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.medium,
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Filled.DeleteOutline, "Eliminar")
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar Guía", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isLoading) showDeleteDialog = false },
                    title = { Text("Confirmar Eliminación") },
                    text = { Text("¿Estás seguro de que deseas eliminar esta guía? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    val result = guideRepository.deleteGuide(guide.id)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        showDeleteDialog = false
                                        navigator.pop()
                                    } else {
                                        showDeleteDialog = false
                                        snackbarController.showError(result.exceptionOrNull()?.message ?: "Error al eliminar")
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.StatusErrorColor)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Eliminar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            enabled = !isLoading
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
            if (showApproveDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isLoading) showApproveDialog = false },
                    title = { Text("Confirmar Aprobación") },
                    text = { Text("¿Estás seguro de que deseas aprobar esta guía? Los datos se marcarán como 'Aprobados' y se enviarán al sistema SQL.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    val result = guideRepository.approveGuide(guide)
                                    isLoading = false

                                    if (result.isSuccess) {
                                        showApproveDialog = false
                                        navigator.pop()
                                    } else {
                                        showApproveDialog = false
                                        snackbarController.showError(
                                            result.exceptionOrNull()?.message ?: "Error al aprobar"
                                        )
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.VetPrimaryColor
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Aprobar")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showApproveDialog = false },
                            enabled = !isLoading
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(onBackClick: () -> Unit, guideName: String) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Detalle de Guía",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.VetTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    guideName,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.VetTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = AppColors.VetPrimaryColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = AppColors.VetTextPrimary
        )
    )
}

@Composable
private fun HeaderCard(guide: RemissionGuide) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AppColors.VetPrimaryColor, AppColors.VetSecondaryColor)
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = "Guía",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = guide.extractedData["numeroGuia"] ?: guide.guideName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = guide.date,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalSectionCard(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppColors.VetPrimaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = AppColors.VetPrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.VetTextPrimary
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.VetTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun EnhancedInfoRow(icon: ImageVector, label: String, value: String?) {
    if (!value.isNullOrBlank() && value != "N/A") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppColors.VetSecondaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.VetTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.VetTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
@Composable
private fun StatusBadge(status: String) {
    val (statusColor, statusIcon, statusText) = when (status) {
        GuideStatus.PENDING_REVIEW -> Triple(
            AppColors.StatusPendingColor,
            Icons.Outlined.PendingActions,
            "Pendiente de Revisión"
        )
        GuideStatus.APPROVED -> Triple(
            AppColors.StatusApprovedColor,
            Icons.Outlined.CheckCircle,
            "Aprobado (Pendiente Sincronización)"
        )
        GuideStatus.SYNCED -> Triple(
            AppColors.StatusSyncedColor,
            Icons.Outlined.DoneAll,
            "Sincronizado con SQL"
        )
        GuideStatus.ERROR -> Triple(
            AppColors.StatusErrorColor,
            Icons.Outlined.ErrorOutline,
            "Error de Sincronización"
        )
        else -> Triple(
            Color.Gray,
            Icons.Filled.Info,
            status.replaceFirstChar { it.titlecase() }
        )
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = statusColor.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Estado",
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = AppColors.VetTextSecondary
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.VetTextPrimary
                )
            }
        }
    }
}

@Composable
private fun ProductTableHeader() {
    Surface(
        color = AppColors.VetPrimaryColor.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Descripción",
                modifier = Modifier.weight(3f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppColors.VetTextPrimary
            )
            Text(
                text = "Cantidad",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppColors.VetTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Unidad",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AppColors.VetTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfessionalProductCard(product: Producto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.VetAccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Medication,
                    contentDescription = null,
                    tint = AppColors.VetSecondaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = product.descripcion ?: "N/A",
                modifier = Modifier.weight(3f),
                fontSize = 14.sp,
                color = AppColors.VetTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = AppColors.VetPrimaryColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.cantidad?.let {
                        if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString()
                    } ?: "-",
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.VetTextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = product.unidad ?: "-",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = AppColors.VetTextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
private fun EditableProductCard(
    product: Producto,
    onProductChange: (Producto) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppColors.VetPrimaryColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = product.descripcion ?: "",
                    onValueChange = { onProductChange(product.copy(descripcion = it)) },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = product.cantidad?.toString() ?: "",
                        onValueChange = {
                            val newAmount = if (it.isEmpty()) null else it.toDoubleOrNull()
                            if (it.isEmpty() || newAmount != null) {
                                onProductChange(product.copy(cantidad = newAmount))
                            }
                        },
                        label = { Text("Cant.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = product.unidad ?: "",
                        onValueChange = { onProductChange(product.copy(unidad = it)) },
                        label = { Text("Unidad") },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    "Eliminar producto",
                    tint = AppColors.StatusErrorColor
                )
            }
        }
    }
}


@Composable
private fun EmptyProductsView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppColors.VetAccentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = AppColors.VetSecondaryColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = AppColors.VetTextSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}