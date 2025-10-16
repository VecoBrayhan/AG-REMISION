package presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import presentation.guide.HistoryScreen
import presentation.guide.UploadGuideScreen
import presentation.settings.SettingsScreen

object MainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { navigator.push(UploadGuideScreen) }) {
                Text("Subir Guía de Remisión")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navigator.push(HistoryScreen) }) {
                Text("Historial de Guías")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navigator.push(SettingsScreen) }) {
                Text("Configuración")
            }
        }
    }
}