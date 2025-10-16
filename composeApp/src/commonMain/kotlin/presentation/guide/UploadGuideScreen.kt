package presentation.guide

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import data.FirestoreGuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.launch

object UploadGuideScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val guideRepository = remember { FirestoreGuideRepository() }
        val scope = rememberCoroutineScope()
        var extractedData by remember { mutableStateOf(mapOf<String, String>()) }
        var isEditing by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = {
                extractedData = mapOf("RUC" to "12345678901", "Fecha" to "14/10/2025")
                isEditing = false
            }) {
                Text("Seleccionar Archivo")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (extractedData.isNotEmpty()) {
                extractedData.forEach { (key, value) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { /* Lógica de edición */ },
                        label = { Text(key) },
                        readOnly = !isEditing
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { isEditing = true }) { Text("Editar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { /* Lógica para extraer datos */ }) { Text("Extraer Datos") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val guide = RemissionGuide(
                                guideName = "Guía Generada",
                                date = extractedData["Fecha"] ?: "",
                                ruc = extractedData["RUC"] ?: "",
                                status = "Enviada",
                                extractedData = extractedData
                            )
                            guideRepository.addGuide(guide)
                            navigator.pop()
                        }
                    }) {
                        Text("Guardar y Enviar")
                    }
                }
            }
        }
    }
}