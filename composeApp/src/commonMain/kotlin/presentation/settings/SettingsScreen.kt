package presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import data.FirebaseAuthRepository
import data.FirestoreUserRepository
import domain.AuthRepository
import domain.UserRepository
import domain.model.UserProfile
import kotlinx.coroutines.launch
import presentation.auth.LoginScreen
import presentation.components.*
import utils.translateError

object SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val authRepository: AuthRepository = remember { FirebaseAuthRepository() }
        val userRepository: UserRepository = remember { FirestoreUserRepository(authRepository) }
        val localNavigator = LocalNavigator.currentOrThrow
        val navigator = localNavigator.parent ?: localNavigator
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()
        var user by remember { mutableStateOf<UserProfile?>(null) }
        var showEditSheet by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showChangePasswordSheet by remember { mutableStateOf(false) }
        var isLoadingUser by remember { mutableStateOf(true) }
        var isSavingProfile by remember { mutableStateOf(false) }
        var isDeletingAccount by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isLoadingUser = true
            try {
                user = userRepository.getCurrentUser()
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarController.showError("Error al cargar el perfil: ${e.message}")
            } finally {
                isLoadingUser = false
            }
        }

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
            containerColor = Color.White
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    if (isLoadingUser) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        UserProfileCard(
                            user = user,
                            onEditClick = { if (user != null) showEditSheet = true }
                        )
                    }
                }
                item {
                    AccountCard(
                        onChangePasswordClick = { showChangePasswordSheet = true },
                        onDeleteAccountClick = { showDeleteDialog = true },
                        onLogoutClick = {
                            scope.launch {
                                authRepository.logout()
                                authRepository.signOutGoogle()
                                navigator.replaceAll(LoginScreen)
                            }
                        }
                    )
                }
            }
        }

        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { if (!isSavingProfile) showEditSheet = false }
            ) {
                EditProfileSheetContent(
                    initialName = user?.name ?: "",
                    initialPhotoUrl = user?.photoUrl ?: "",
                    onCancel = { showEditSheet = false },
                    onSave = { newName, newPhotoUrl ->
                        scope.launch {
                            isSavingProfile = true
                            try {
                                user?.let { current ->
                                    val updated = current.copy(
                                        name = newName,
                                        photoUrl = newPhotoUrl
                                    )
                                    val result = userRepository.updateUserProfile(updated)
                                    if (result.isSuccess) {
                                        user = updated // <-- Actualiza el estado local
                                        showEditSheet = false
                                        snackbarController.showSuccess("Perfil actualizado.")
                                    } else {
                                        snackbarController.showError(
                                            "Error: ${translateError(result.exceptionOrNull()?.message)}"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                snackbarController.showError("Error: ${translateError(e.message)}")
                            } finally {
                                isSavingProfile = false
                            }
                        }
                    }
                )
            }
        }
        if (showChangePasswordSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChangePasswordSheet = false }
            ) {
                ChangePasswordSheetContent(
                    onCancel = { showChangePasswordSheet = false },
                    onSave = { newPassword ->
                        scope.launch {
                            try {
                                authRepository.updatePassword(newPassword)
                                snackbarController.showSuccess("Contraseña actualizada correctamente.")
                                showChangePasswordSheet = false
                            } catch (e: Exception) {
                                snackbarController.showError("Error: ${translateError(e.message)}")
                            }
                        }
                    }
                )
            }
        }
        if (showDeleteDialog) {
            DeleteAccountDialog(
                isDeleting = isDeletingAccount,
                onConfirm = {
                    scope.launch {
                        isDeletingAccount = true
                        try {
                            val result = authRepository.deleteCurrentUser()
                            authRepository.signOutGoogle()
                            if (result.isSuccess) {
                                showDeleteDialog = false
                                navigator.replaceAll(LoginScreen) // <-- Usa el navegador corregido
                            } else {
                                snackbarController.showError("Error: ${translateError(result.exceptionOrNull()?.message)}")
                            }
                        } catch (e: Exception) {
                            snackbarController.showError("Error: ${translateError(e.message)}")
                        } finally {
                            isDeletingAccount = false
                        }
                    }
                },
                onDismiss = { if (!isDeletingAccount) showDeleteDialog = false }
            )
        }
    }
}


@Composable
private fun UserProfileCard(user: UserProfile?, onEditClick: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(95.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user?.name ?: "Usuario",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.1f
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = user?.email ?: "Sin correo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            LoadingActionButtonComponent(
                text = "Editar perfil",
                isLoading = isLoading,
                isEnabled = true,
                onClick = {
                    isLoading = true
                    try {
                        onEditClick()
                    } finally {
                        isLoading = false
                    }
                },
                modifier = Modifier.height(50.dp)
            )
        }
    }
}

@Composable
private fun AccountCard(
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            AccountActionRow(
                icon = Icons.Default.Lock,
                text = "Cambiar contraseña",
                onClick = onChangePasswordClick
            )
            Divider()

            AccountActionRow(
                icon = Icons.Default.DeleteForever,
                text = "Eliminar cuenta",
                color = MaterialTheme.colorScheme.error,
                onClick = onDeleteAccountClick
            )

            Spacer(Modifier.height(16.dp))
            LoadingActionButtonComponent(
                text = "Cerrar sesión",
                isLoading = false,
                isEnabled = true,
                onClick = onLogoutClick,
                modifier = Modifier.height(50.dp)
            )
        }
    }
}

@Composable
private fun AccountActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val effectiveColor = color.takeOrElse { LocalContentColor.current }
    val displayColor = if (enabled) effectiveColor else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = displayColor)
            Spacer(Modifier.width(16.dp))
        } else Spacer(Modifier.width(40.dp))

        Text(text, Modifier.weight(1f), color = displayColor, style = MaterialTheme.typography.bodyLarge)

        if (enabled && onClick != null) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun EditProfileSheetContent(
    initialName: String,
    initialPhotoUrl: String,
    onCancel: () -> Unit,
    onSave: (newName: String, newPhotoUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var photoUrl by remember { mutableStateOf(initialPhotoUrl) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Editar Perfil", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(model = photoUrl, contentDescription = "Foto de perfil", modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto por defecto",
                        tint = Color.Gray,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomTextField(value = name, onValueChange = { name = it }, label = "Nombre completo")
                CustomTextField(value = photoUrl, onValueChange = { photoUrl = it }, label = "URL de foto (opcional)")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ButtonComponentDialog(text = "Cancelar", onClick = onCancel, isPrimary = false)
            Spacer(Modifier.width(12.dp))
            ButtonComponentDialog(text = "Guardar", onClick = { onSave(name, photoUrl) })
        }
    }
}
@Composable
private fun ChangePasswordSheetContent(
    onCancel: () -> Unit,
    onSave: suspend (newPassword: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Cambiar contraseña",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        CustomTextField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                errorMessage = null
            },
            label = "Nueva Contraseña",
            isPasswordField = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible }
        )
        CustomTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = "Confirmar Contraseña",
            isPasswordField = true,
            isPasswordVisible = isConfirmPasswordVisible,
            onPasswordToggleClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
        )
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            ButtonComponentDialog(
                text = "Cancelar",
                onClick = onCancel,
                isPrimary = false,
            )
            Spacer(Modifier.width(12.dp))
            ButtonComponentDialog(
                text = "Guardar",
                onClick = {
                    when {
                        newPassword.isBlank() || confirmPassword.isBlank() -> {
                            errorMessage = "Completa ambos campos."
                        }

                        newPassword != confirmPassword -> {
                            errorMessage = "Las contraseñas no coinciden."
                        }

                        newPassword.length < 6 -> {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                        }

                        else -> {
                            scope.launch {
                                isLoading = true
                                try {
                                    onSave(newPassword)
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${translateError(e.message)}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}


@Composable
private fun DeleteAccountDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { Text("Eliminar Cuenta") },
        text = { Text("¿Estás seguro? Esta acción eliminará todos tus datos permanentemente.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isDeleting)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                else Text("Eliminar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}