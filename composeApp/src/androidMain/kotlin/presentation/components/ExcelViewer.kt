package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import kotlin.math.max

private typealias ExcelSheetData = List<List<String>>

private sealed class ExcelState {
    object Loading : ExcelState()
    data class Success(val data: ExcelSheetData, val columnCount: Int) : ExcelState()
    data class Error(val message: String) : ExcelState()
}

@Composable
actual fun ExcelViewer(
    modifier: Modifier,
    excelBytes: ByteArray
) {
    val state by produceState<ExcelState>(ExcelState.Loading, excelBytes) {
        value = withContext(Dispatchers.IO) {
            try {
                excelBytes.inputStream().use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    if (workbook.numberOfSheets == 0) {
                        return@withContext ExcelState.Error("Error: El archivo Excel no contiene hojas.")
                    }
                    val sheet = workbook.getSheetAt(0)
                    if (sheet == null || sheet.physicalNumberOfRows == 0) {
                        return@withContext ExcelState.Error("Error: La primera hoja del Excel está vacía.")
                    }
                    val formatter = DataFormatter()
                    var maxCols = 0

                    val data = sheet.mapNotNull { row ->
                        val rowData = row.map { cell ->
                            formatter.formatCellValue(cell) ?: ""
                        }
                        maxCols = max(maxCols, rowData.size)
                        if (rowData.any { it.isNotBlank() }) rowData else null
                    }
                    workbook.close()

                    if (data.isEmpty()) {
                        ExcelState.Error("Error: No se encontraron datos válidos en la hoja.")
                    } else {
                        ExcelState.Success(data, maxCols)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ExcelState.Error("Error al procesar el archivo Excel:\n${e.localizedMessage}")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val currentState = state) {
            is ExcelState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cargando Excel...")
                }
            }
            is ExcelState.Error -> {
                Text(
                    text = currentState.message,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            is ExcelState.Success -> {
                if (currentState.data.isEmpty()) {
                    Text(
                        text = "La hoja parece estar vacía.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    ExcelTable(data = currentState.data, columnCount = currentState.columnCount)
                }
            }
        }
    }
}

@Composable
private fun ExcelTable(modifier: Modifier = Modifier, data: ExcelSheetData, columnCount: Int) {
    val scrollState = rememberScrollState()
    Column(modifier = modifier.horizontalScroll(scrollState)) {

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(data) { rowIndex, rowData ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(columnCount) { colIndex ->
                        val cellData = rowData.getOrNull(colIndex) ?: ""
                        TableCell(text = cellData)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(text: String, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .width(150.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = MaterialTheme.typography.bodySmall.fontSize
    )
}