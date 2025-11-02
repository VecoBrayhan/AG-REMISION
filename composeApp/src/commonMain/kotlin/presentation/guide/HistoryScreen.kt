package presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirestoreGuideRepository
import domain.RemissionGuide
import presentation.components.DateRangeSelector
import kotlinx.datetime.*
import presentation.components.TopBarComponent
import presentation.components.TopBarType
import utils.AppColors
import utils.filterGuidesByDate

object HistoryScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        Navigator(HistoryListContentScreen)
    }
}

private object HistoryListContentScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val guideRepository = remember { FirestoreGuideRepository() }
        val allGuides by guideRepository.getGuides().collectAsState(emptyList())

        var fechaInicioSeleccionada by remember { mutableStateOf<Long?>(null) }
        var fechaFinSeleccionada by remember { mutableStateOf<Long?>(null) }

        val navigator = LocalNavigator.currentOrThrow

        val filteredGuides = remember(allGuides, fechaInicioSeleccionada, fechaFinSeleccionada) {
            filterGuidesByDate(allGuides, fechaInicioSeleccionada, fechaFinSeleccionada)
        }

        Scaffold(
            topBar = {
                TopBarComponent(
                    type = TopBarType.MAIN,
                    title = "Historial de Guías",
                    subtitle = "Gestión de Remisión",
                    icon = Icons.Filled.Pets
                )
            },
            containerColor = AppColors.VetBackgroundLight
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.VetCardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = "Filtros",
                                tint = AppColors.VetPrimaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtrar por Fecha",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.VetTextPrimary
                            )
                        }
                        DateRangeSelector(
                            fechaInicio = fechaInicioSeleccionada,
                            fechaFin = fechaFinSeleccionada,
                            onInicioSelected = { fechaInicioSeleccionada = it },
                            onFinSelected = { fechaFinSeleccionada = it }
                        )
                    }
                }

                if (filteredGuides.isEmpty()) {
                    EmptyStateView(hasFilter = fechaInicioSeleccionada != null || fechaFinSeleccionada != null)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        items(filteredGuides, key = { it.id }) { guide ->
                            ProfessionalGuideCard(
                                guide = guide,
                                onClick = {
                                    navigator.push(GuideDetailScreen(guide))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalGuideCard(
    guide: RemissionGuide,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground)
    ) {
        Column {
            // Header de la tarjeta con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AppColors.VetPrimaryColor, AppColors.VetSecondaryColor)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Business,
                            contentDescription = "Empresa",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guide.extractedData["nombreEmpresa"]
                                ?: guide.extractedData["laboratorio"]
                                ?: guide.guideName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Badge,
                                contentDescription = "RUC",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RUC: ${guide.ruc}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Ver detalle",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Contenido de la tarjeta
            Column(modifier = Modifier.padding(16.dp)) {
                EnhancedInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Fecha Documento",
                    value = guide.date,
                    iconTint = AppColors.VetPrimaryColor
                )
            }
        }
    }
}

@Composable
private fun EnhancedInfoRow(
    icon: ImageVector,
    label: String,
    value: String?,
    iconTint: Color = AppColors.VetTextSecondary
) {
    if (!value.isNullOrBlank() && value != "N/A") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.VetTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.VetTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(hasFilter: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    .background(AppColors.VetAccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasFilter) Icons.Outlined.SearchOff else Icons.Outlined.FolderOpen,
                    contentDescription = "Sin guías",
                    tint = AppColors.VetPrimaryColor,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (hasFilter) "No hay guías en este período" else "No hay guías registradas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.VetTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasFilter)
                    "Intenta ajustar el rango de fechas para ver más resultados"
                else
                    "Las guías de remisión aparecerán aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.VetTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

