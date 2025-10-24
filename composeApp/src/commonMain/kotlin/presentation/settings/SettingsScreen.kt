package presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirebaseAuthRepository
import kotlinx.coroutines.launch
import presentation.auth.LoginScreen
import presentation.components.CustomTextField

object SettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = remember { FirebaseAuthRepository() }
        val scope = rememberCoroutineScope()
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var isConfirmPasswordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween // Alinea los elementos
        ) {
            // Sección para actualizar contraseña
            Column {
                Text("Actualizar Contraseña", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "Nueva Contraseña",
                    isPasswordField = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible }
                )
                Spacer(modifier = Modifier.height(8.dp))

                CustomTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirmar Contraseña",
                    isPasswordField = true,
                    isPasswordVisible = isConfirmPasswordVisible,
                    onPasswordToggleClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                            scope.launch {
                                authRepository.updatePassword(newPassword)
                                // Aquí puedes mostrar un Snackbar o Toast de éxito
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar Cambios")
                }
            }

            // --- Botón de Cerrar Sesión ---
            Button(
                onClick = {
                    scope.launch {
                        authRepository.logout()
                        val parentNavigator = navigator.parent ?: navigator
                        parentNavigator.replaceAll(LoginScreen)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                // Le damos un color distintivo para indicar una acción importante/destructiva
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}