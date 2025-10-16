package presentation.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import data.FirestoreGuideRepository

object HistoryScreen : Screen {
    @Composable
    override fun Content() {
        val guideRepository = remember { FirestoreGuideRepository() }
        val guides by guideRepository.getGuides().collectAsState(emptyList())
        var filterName by remember { mutableStateOf("") }
        var filterDate by remember { mutableStateOf("") }
        var filterRuc by remember { mutableStateOf("") }
        var filterStatus by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(value = filterName, onValueChange = { filterName = it }, label = { Text("Buscar por nombre") })
            OutlinedTextField(value = filterDate, onValueChange = { filterDate = it }, label = { Text("Buscar por fecha") })
            OutlinedTextField(value = filterRuc, onValueChange = { filterRuc = it }, label = { Text("Buscar por RUC") })
            OutlinedTextField(value = filterStatus, onValueChange = { filterStatus = it }, label = { Text("Buscar por estado") })
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(guides.filter {
                    it.guideName.contains(filterName, ignoreCase = true) &&
                            it.date.contains(filterDate, ignoreCase = true) &&
                            it.ruc.contains(filterRuc, ignoreCase = true) &&
                            it.status.contains(filterStatus, ignoreCase = true)
                }) { guide ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Guía: ${guide.guideName}", style = MaterialTheme.typography.titleMedium)
                            Text("RUC: ${guide.ruc}")
                            Text("Fecha: ${guide.date}")
                            Text("Estado: ${guide.status}")
                        }
                    }
                }
            }
        }
    }
}