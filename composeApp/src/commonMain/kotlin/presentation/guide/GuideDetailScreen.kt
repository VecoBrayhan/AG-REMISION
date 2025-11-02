package presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

// Colores consistentes con HistoryScreen
private val VetPrimaryColor = Color(0xFF2E7D32)
private val VetSecondaryColor = Color(0xFF66BB6A)
private val VetAccentColor = Color(0xFF81C784)
private val VetBackgroundLight = Color(0xFFF1F8F4)
private val VetCardBackground = Color(0xFFFFFFFF)
private val VetTextPrimary = Color(0xFF1B5E20)
private val VetTextSecondary = Color(0xFF558B2F)
private val VetBorderColor = Color(0xFFE0E0E0)

data class GuideDetailScreen(val guide: RemissionGuide) : Screen {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val products: List<Producto> = remember(guide.extractedData) {
            guide.extractedData["productosJson"]?.let { jsonString ->
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
            containerColor = VetBackgroundLight
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Header con información destacada
                item {
                    HeaderCard(guide = guide)
                }

                // Información General
                item {
                    ProfessionalSectionCard(
                        title = "Información General",
                        icon = Icons.Outlined.Info
                    ) {
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
                                label = "Fecha Llegada",
                                value = guide.extractedData["fechaLlegada"]
                            )
                            EnhancedInfoRow(
                                icon = Icons.Outlined.AttachMoney,
                                label = "Costo Transporte",
                                value = guide.extractedData["costoTransporte"]
                            )

                            // Status badge
                            guide.status?.let { status ->
                                if (status.isNotBlank() && status != "N/A") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusBadge(status = status)
                                }
                            }
                        }
                    }
                }

                // Sección de Productos
                if (products.isNotEmpty()) {
                    item {
                        ProfessionalSectionCard(
                            title = "Productos Recibidos",
                            icon = Icons.Outlined.Inventory2,
                            subtitle = "${products.size} producto${if (products.size != 1) "s" else ""}"
                        ) {
                            // Header de tabla
                            ProductTableHeader()
                        }
                    }

                    // Lista de productos
                    items(products) { product ->
                        ProfessionalProductCard(product)
                    }

                } else if (guide.extractedData.containsKey("productosJson") || guide.extractedData.containsKey("productosError")) {
                    item {
                        ProfessionalSectionCard(
                            title = "Productos",
                            icon = Icons.Outlined.Inventory2
                        ) {
                            EmptyProductsView(
                                message = guide.extractedData["productosError"]
                                    ?: "No se encontraron productos en esta guía."
                            )
                        }
                    }
                }

                // Espacio final
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
                    color = VetTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    guideName,
                    style = MaterialTheme.typography.bodySmall,
                    color = VetTextSecondary,
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
                    tint = VetPrimaryColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = VetTextPrimary
        )
    )
}

@Composable
private fun HeaderCard(guide: RemissionGuide) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(VetPrimaryColor, VetSecondaryColor)
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
        colors = CardDefaults.cardColors(containerColor = VetCardBackground)
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
                        .background(VetPrimaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = VetPrimaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = VetTextPrimary
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = VetTextSecondary,
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
                tint = VetSecondaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = VetTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = VetTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when (status.lowercase()) {
            "completado", "entregado", "recibido" -> VetSecondaryColor.copy(alpha = 0.15f)
            "pendiente", "en proceso" -> Color(0xFFFFA726).copy(alpha = 0.15f)
            else -> Color.Gray.copy(alpha = 0.15f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status.lowercase()) {
                    "completado", "entregado", "recibido" -> Icons.Filled.CheckCircle
                    "pendiente", "en proceso" -> Icons.Filled.Schedule
                    else -> Icons.Filled.Info
                },
                contentDescription = "Estado",
                tint = when (status.lowercase()) {
                    "completado", "entregado", "recibido" -> VetSecondaryColor
                    "pendiente", "en proceso" -> Color(0xFFFFA726)
                    else -> Color.Gray
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = VetTextSecondary
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = VetTextPrimary
                )
            }
        }
    }
}

@Composable
private fun ProductTableHeader() {
    Surface(
        color = VetPrimaryColor.copy(alpha = 0.08f),
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
                color = VetTextPrimary
            )
            Text(
                text = "Cantidad",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = VetTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Unidad",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = VetTextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfessionalProductCard(product: Producto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono decorativo
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VetAccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Medication,
                    contentDescription = null,
                    tint = VetSecondaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Descripción
            Text(
                text = product.descripcion ?: "N/A",
                modifier = Modifier.weight(3f),
                fontSize = 14.sp,
                color = VetTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Cantidad
            Surface(
                color = VetPrimaryColor.copy(alpha = 0.1f),
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
                    color = VetTextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Unidad
            Text(
                text = product.unidad ?: "-",
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = VetTextSecondary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
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
                .background(VetAccentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = VetSecondaryColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = VetTextSecondary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}