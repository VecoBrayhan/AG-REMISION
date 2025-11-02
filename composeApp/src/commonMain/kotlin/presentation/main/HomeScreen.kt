package presentation.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import kotlinx.datetime.*
import utils.AppColors

@Composable
fun HomeScreen() {
    val tabNavigator = LocalTabNavigator.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.VetBackgroundLight)
            .verticalScroll(rememberScrollState())
    ) {
        // Header con gradiente
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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            "Panel de Control - Gestión Veterinaria",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pets,
                            contentDescription = "Veterinaria",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Estadísticas Rápidas
            Text(
                "Estadísticas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.VetTextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Procesadas Hoy",
                    value = "12",
                    icon = Icons.Outlined.CheckCircle,
                    color = AppColors.VetSecondaryColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Este Mes",
                    value = "13",
                    icon = Icons.Outlined.CalendarMonth,
                    color = AppColors.VetPrimaryColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Acciones Rápidas
            Text(
                "Acciones Rápidas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.VetTextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfessionalQuickActionCard(
                    title = "Subir Nueva Guía",
                    description = "Cargar y procesar documentos de remisión",
                    icon = Icons.Outlined.CloudUpload,
                    gradient = listOf(AppColors.VetPrimaryColor, AppColors.VetSecondaryColor),
                    onClick = { tabNavigator.current = UploadTab }
                )

                ProfessionalQuickActionCard(
                    title = "Ver Historial Completo",
                    description = "Revisar todas las guías registradas",
                    icon = Icons.Outlined.History,
                    gradient = listOf(AppColors.VetSecondaryColor, AppColors.VetAccentColor),
                    onClick = { tabNavigator.current = HistoryTab }
                )

                ProfessionalQuickActionCard(
                    title = "Buscar Productos",
                    description = "Consultar inventario de productos recibidos",
                    icon = Icons.Outlined.Search,
                    gradient = listOf(Color(0xFF43A047), Color(0xFF66BB6A)),
                    onClick = { /* Implementar búsqueda */ }
                )
            }

            Spacer(Modifier.height(24.dp))

        }
    }
}



@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.VetTextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProfessionalQuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = AppColors.VetCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppColors.VetTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = AppColors.VetTextSecondary,
                    lineHeight = 16.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(gradient[0].copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = gradient[0],
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

