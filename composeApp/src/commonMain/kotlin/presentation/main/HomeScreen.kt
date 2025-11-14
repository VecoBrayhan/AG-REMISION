@file:Suppress("UNRESOLVED_REFERENCE")
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
import presentation.components.TopBarComponent
import presentation.components.TopBarType
import utils.AppColors
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val tabNavigator = LocalTabNavigator.current
    Scaffold(
        topBar = {
            TopBarComponent(
                type = TopBarType.MAIN,
                title = "Panel de Control",
                subtitle = "Gestión Veterinaria",
                icon = Icons.Filled.Pets
            )
        },
        containerColor = AppColors.VetBackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

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
            }
            Spacer(Modifier.height(24.dp))
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

