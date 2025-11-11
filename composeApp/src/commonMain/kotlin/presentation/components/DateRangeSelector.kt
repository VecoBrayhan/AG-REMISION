package presentation.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import utils.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    fechaInicio: Long?,
    fechaFin: Long?,
    onInicioSelected: (Long?) -> Unit,
    onFinSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val datePickerStateStart = rememberDatePickerState(initialSelectedDateMillis = fechaInicio)
    val datePickerStateEnd = rememberDatePickerState(initialSelectedDateMillis = fechaFin)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateButton(
                text = fechaInicio?.millisToDmy() ?: "Desde",
                onClick = { showStartPicker = true }
            )

            Text("-", color = Color.Gray)

            DateButton(
                text = fechaFin?.millisToDmy() ?: "Hasta",
                onClick = { showEndPicker = true }
            )

            TextButton(
                onClick = {
                    onInicioSelected(null)
                    onFinSelected(null)
                    datePickerStateStart.selectedDateMillis = null
                    datePickerStateEnd.selectedDateMillis = null
                },
                enabled = fechaInicio != null || fechaFin != null
            ) {
                Text(
                    "Limpiar",
                    color = if (fechaInicio != null || fechaFin != null) AppColors.primaryColor else Color.Gray
                )
            }
        }
    }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onInicioSelected(datePickerStateStart.selectedDateMillis?.startOfDayMillis())
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStateStart) }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onFinSelected(datePickerStateEnd.selectedDateMillis?.endOfDayMillis())
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStateEnd) }
    }
}

@Composable
private fun DateButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.primaryColor),
        border = BorderStroke(1.dp, AppColors.primaryColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp)
    }
}

private fun Long.millisToDmy(): String {
    val tz = TimeZone.UTC
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).date
    return "${date.dayOfMonth.pad()}/${date.monthNumber.pad()}/${date.year}"
}

private fun Long.startOfDayMillis(): Long {
    val tz = TimeZone.UTC
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).date
    val ldt = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 0, 0, 0, 0)
    return ldt.toInstant(tz).toEpochMilliseconds()
}

private fun Long.endOfDayMillis(): Long {
    val tz = TimeZone.UTC
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).date
    val ldt = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, 23, 59, 59, 999_999_999)
    return ldt.toInstant(tz).toEpochMilliseconds()
}

private fun Int.pad(): String = this.toString().padStart(2, '0')
