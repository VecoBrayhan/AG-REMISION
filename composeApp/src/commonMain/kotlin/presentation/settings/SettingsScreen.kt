package presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirebaseAuthRepository
import dev.gitlive.firebase.Firebase // Import Firebase
import dev.gitlive.firebase.auth.auth // Import Firebase Auth
import kotlinx.coroutines.launch
import presentation.auth.LoginScreen
import presentation.components.CustomTextField
import presentation.components.LoadingActionButtonComponent // Importa tu botón
import presentation.components.ReusableSnackbarHost // Importa el Snackbar
import presentation.components.TopBarComponent
import presentation.components.TopBarType
import presentation.components.rememberSnackbarController

// --- Colores (traídos de tus otros archivos para consistencia) ---
private val VetPrimaryColor = Color(0xFF2E7D32)
private val VetSecondaryColor = Color(0xFF66BB6A)
private val VetBackgroundLight = Color(0xFFF1F8F4)
private val VetCardBackground = Color(0xFFFFFFFF)
private val VetTextPrimary = Color(0xFF1B5E20)
private val VetTextSecondary = Color(0xFF558B2F)
// -----------------------------------------------------------------

object SettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = remember { FirebaseAuthRepository() }
        val scope = rememberCoroutineScope()
        val snackbarController = rememberSnackbarController()

        // Obtener el usuario actual de Firebase Auth
        val currentUser = remember { Firebase.auth.currentUser }

        // Estados para la actualización de contraseña
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isNewPasswordVisible by remember { mutableStateOf(false) }
        var isConfirmPasswordVisible by remember { mutableStateOf(false) }
        var isPasswordLoading by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopBarComponent(
                    type = TopBarType.MAIN,
                    title = "Configuración",
                    subtitle = "Perfil y seguridad",
                    icon = Icons.Filled.Settings
                )
            },
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) },
            containerColor = VetBackgroundLight // Color de fondo consistente
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()) // Para que la pantalla sea desplazable
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. Sección de Perfil de Usuario ---
                UserProfileHeader(
                    name = currentUser?.displayName,
                    email = currentUser?.email
                    // photoUrl = currentUser?.photoURL // Pasamos la URL (ver Composable)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. Sección de Actualizar Contraseña ---
                PasswordUpdateCard(
                    newPassword = newPassword,
                    onNewPasswordChange = { newPassword = it },
                    isNewPasswordVisible = isNewPasswordVisible,
                    onNewPasswordToggle = { isNewPasswordVisible = !isNewPasswordVisible },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    isConfirmPasswordVisible = isConfirmPasswordVisible,
                    onConfirmPasswordToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    isLoading = isPasswordLoading,
                    onUpdateClick = {
                        if (newPassword.isBlank() || confirmPassword.isBlank()) {
                            snackbarController.showError("Por favor, completa ambos campos.")
                            return@PasswordUpdateCard
                        }
                        if (newPassword != confirmPassword) {
                            snackbarController.showError("Las contraseñas no coinciden.")
                            return@PasswordUpdateCard
                        }
                        if (newPassword.length < 6) {
                            snackbarController.showError("La contraseña debe tener al menos 6 caracteres.")
                            return@PasswordUpdateCard
                        }

                        scope.launch {
                            isPasswordLoading = true
                            val result = authRepository.updatePassword(newPassword)
                            if (result.isSuccess) {
                                snackbarController.showSuccess("Contraseña actualizada exitosamente.")
                                // Limpiar campos
                                newPassword = ""
                                confirmPassword = ""
                            } else {
                                snackbarController.showError(result.exceptionOrNull()?.message ?: "Error al actualizar")
                            }
                            isPasswordLoading = false
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. Sección de Cerrar Sesión ---
                LogoutButton(
                    onClick = {
                        scope.launch {
                            authRepository.logout()
                            // Importante: También cerrar sesión de Google si se usó
                            authRepository.signOutGoogle()
                            // Vuelve al Login y limpia la pila de navegación
                            val parentNavigator = navigator.parent ?: navigator
                            parentNavigator.replaceAll(LoginScreen)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Muestra el avatar (con inicial) y los datos del usuario.
 */
@Composable
private fun UserProfileHeader(name: String?, email: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar del Usuario
        val initial = name?.trim()?.firstOrNull()?.uppercaseChar() ?: '?'
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(VetPrimaryColor, VetSecondaryColor)
                    )
                )
                .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // NOTA: Para cargar 'photoUrl' necesitarías una librería como Coil
            // (io.coil-kt.coil-compose). Por ahora, usamos la inicial.
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre
        Text(
            text = name ?: "Usuario de AG-Remisión",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = VetTextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Correo
        Text(
            text = email ?: "No se encontró correo",
            style = MaterialTheme.typography.bodyLarge,
            color = VetTextSecondary
        )
    }
}

/**
 * Tarjeta para la sección de "Actualizar Contraseña".
 */
@Composable
private fun PasswordUpdateCard(
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    isNewPasswordVisible: Boolean,
    onNewPasswordToggle: () -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isConfirmPasswordVisible: Boolean,
    onConfirmPasswordToggle: () -> Unit,
    isLoading: Boolean,
    onUpdateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = VetCardBackground),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                icon = Icons.Filled.Lock,
                text = "Actualizar Contraseña"
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "Nueva Contraseña",
                isPasswordField = true,
                isPasswordVisible = isNewPasswordVisible,
                onPasswordToggleClick = onNewPasswordToggle
            )

            Spacer(modifier = Modifier.height(8.dp))

            CustomTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirmar Contraseña",
                isPasswordField = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onPasswordToggleClick = onConfirmPasswordToggle
            )

            Spacer(modifier = Modifier.height(24.dp))

            LoadingActionButtonComponent(
                text = "Guardar Cambios",
                isLoading = isLoading,
                isEnabled = !isLoading,
                onClick = onUpdateClick,
                icon = Icons.Filled.Save,
                loadingText = "Guardando..."
            )
        }
    }
}

/**
 * Botón para "Cerrar Sesión".
 */
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Text("Cerrar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/**
 * Título reutilizable para las secciones de la tarjeta.
 */
@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = VetPrimaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = VetTextPrimary
        )
    }
}
