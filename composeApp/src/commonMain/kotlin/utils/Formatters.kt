package utils
import domain.RemissionGuide
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.pow
import kotlin.math.round


 fun formatFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${roundToTwoDecimals(bytes / (1024f * 1024f))} MB"
        else -> "${roundToTwoDecimals(bytes / (1024f * 1024f * 1024f))} GB"
    }
}

 fun roundToTwoDecimals(value: Float): String {
    val rounded = round(value * 100) / 100
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()  // Evita ".0"
    } else {
        rounded.toString()
    }
}


private fun formatIsoWithSplit(fechaIso: String): String {
    return try {
        val (datePart, timePartRaw) = fechaIso.split('T', limit = 2)
        val (yearStr, monthStr, dayStr) = datePart.split('-', limit = 3)
        val day = dayStr.toInt()
        val month = monthStr.toInt()
        val year = yearStr.toInt()
        val timeClean = timePartRaw.takeWhile { it.isDigit() || it == ':' }
        val timePieces = timeClean.split(':', limit = 3)
        val hour24 = timePieces.getOrNull(0)?.toInt() ?: 0
        val minute = timePieces.getOrNull(1)?.toInt() ?: 0
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val dd = day.toString().padStart(2, '0')
        val MM = month.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')
        "$dd/$MM/$year $hour12:$mm $amPm"
    } catch (e: Exception) {
        fechaIso
    }
}


private fun Int.pad(): String = this.toString().padStart(2, '0')

fun Long.millisecondsToSecondsString(): String {
    val seconds = this / 1000.0
    val rounded = round(seconds * 100) / 100
    return "$rounded s"
}

fun Double.toFormattedString(decimals: Int = 2): String {
    val factor = 10.0.pow(decimals)
    return (round(this * factor) / factor).toString()
}

fun formatTiempoAtras(fechaRegistro: Long): String {
    val ahora = Clock.System.now().toEpochMilliseconds()
    val diffMillis = ahora - fechaRegistro
    val diffSegundos = (diffMillis / 1000).toInt()
    val diffMinutos = diffSegundos / 60
    val diffHoras = diffMinutos / 60
    val diffDias = diffHoras / 24

    return when {
        diffDias > 0 -> "$diffDias día${if (diffDias > 1) "s" else ""} atrás"
        diffHoras > 0 -> "$diffHoras hora${if (diffHoras > 1) "s" else ""} atrás"
        diffMinutos > 0 -> "$diffMinutos min atrás"
        else -> "Justo ahora"
    }
}

fun String.truncarNombre(maximoCaracteres: Int = 30): String {
    return if (this.length > maximoCaracteres) {
        this.substring(0, maximoCaracteres) + "..."
    } else {
        this
    }
}

fun Long.toReadableTime(): String {
    val minutos = (this / 60).toInt()
    val segundos = (this % 60).toInt()
    return if (minutos > 0) {
        "$minutos min ${segundos}s"
    } else {
        "$segundos s"
    }
}

fun Long.millisecondsToSeconds(): Double {
    return this / 1000.0
}

fun parseGuideDate(dateString: String): LocalDate? {
    return try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        println("Error parseando fecha '$dateString': ${e.message}")
        null
    }
}

fun filterGuidesByDate(
    guides: List<RemissionGuide>,
    startDateMillis: Long?,
    endDateMillis: Long?
): List<RemissionGuide> {
    if (startDateMillis == null && endDateMillis == null) {
        return guides
    }

    val startDate = startDateMillis?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
    }
    val endDate = endDateMillis?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
    }

    return guides.filter { guide ->
        val guideDate = parseGuideDate(guide.date) ?: return@filter false
        val afterStartDate = startDate == null || guideDate >= startDate
        val beforeEndDate = endDate == null || guideDate <= endDate
        afterStartDate && beforeEndDate
    }
}
