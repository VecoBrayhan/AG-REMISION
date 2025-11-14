package utils
import domain.RemissionGuide
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

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
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}
fun formatIsoToReadableDate(fechaIso: String): String {
    return try {
        val instant = Instant.parse(fechaIso)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        // Define el patrón de formateo
        val formatter = LocalDateTime.Format {
            dayOfMonth(Padding.ZERO)
            char('/')
            monthNumber(Padding.ZERO)
            char('/')
            year()
            char(' ')
            amPmHour(Padding.ZERO)
            char(':')
            minute(Padding.ZERO)
            amPmMarker("am", "pm")
        }
        localDateTime.format(formatter)
    } catch (e: Exception) {
        fechaIso
    }
}

fun parseGuideDate(dateString: String): LocalDate? {
    return try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
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
