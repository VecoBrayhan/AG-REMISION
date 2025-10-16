package presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirebaseAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import presentation.components.CustomTextField
import presentation.components.LoadingButton
import presentation.components.ReusableSnackbarHost
import presentation.components.rememberSnackbarController
import utils.translateError
object RegisterScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository = remember { FirebaseAuthRepository() }
        val snackbarController = rememberSnackbarController()
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = { ReusableSnackbarHost(controller = snackbarController) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                CustomTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(value = email, onValueChange = { email = it }, label = "Correo Electrónico")
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(
                    value = password, onValueChange = { password = it }, label = "Contraseña",
                    isPasswordField = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible }
                )
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(
                    value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirmar Contraseña",
                    isPasswordField = true,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordToggleClick = { isPasswordVisible = !isPasswordVisible }
                )
                Spacer(modifier = Modifier.height(16.dp))
                LoadingButton(
                    text = "Registrar",
                    isLoading = isLoading,
                    onClick = {
                        if (password != confirmPassword) {
                            snackbarController.showError("Las contraseñas no coinciden.")
                        } else {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = authRepository.register(name.trim(), email.trim(), password)
                                    if (result.isSuccess) {
                                        snackbarController.showSuccess("¡Registro exitoso! Revisa tu correo para verificar tu cuenta.")
                                        delay(2000L)
                                        navigator.replaceAll(LoginScreen)
                                    } else {
                                        val errorMessage = translateError(result.exceptionOrNull()?.message)
                                        snackbarController.showError(errorMessage)
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿Ya tienes cuenta? ")
                    TextButton(onClick = { navigator.push(LoginScreen) }) {
                        Text("Iniciar sesión")
                    }
                }
            }
        }
    }
}