package presentation.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import utils.AppColors

enum class TopBarType {
    MAIN, DETAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarComponent(
    type: TopBarType = TopBarType.MAIN,
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onBackClick: (() -> Unit)? = null
) {
    val navigator = LocalNavigator.currentOrThrow.takeIf { type == TopBarType.DETAIL }
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (type == TopBarType.MAIN && icon == null && onBackClick == null) {
                    Spacer(modifier = Modifier.width(56.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.VetTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.VetTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            when (type) {
                TopBarType.DETAIL -> {
                    IconButton(onClick = {
                        onBackClick?.invoke() ?: navigator?.pop()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retroceder",
                            tint = AppColors.VetTextPrimary
                        )
                    }
                }
                TopBarType.MAIN -> {
                    icon?.let {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(AppColors.VetPrimaryColor, AppColors.VetSecondaryColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = it,
                                contentDescription = title,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } ?: Spacer(modifier = Modifier.width(16.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = AppColors.VetTextPrimary
        ),
        modifier = Modifier
    )
}