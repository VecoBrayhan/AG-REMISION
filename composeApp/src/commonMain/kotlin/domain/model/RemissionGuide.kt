package domain.model

import kotlinx.serialization.Serializable
import kotlin.text.get

object GuideStatus {
    const val PENDING_REVIEW = "PENDIENTE_REVISION"
    const val APPROVED = "APROBADO"
    const val SYNCED = "SINCRONIZADO"
    const val ERROR = "ERROR"
}

@Serializable
data class RemissionGuide(
    val id: String = "",
    val guideName: String = "",
    val date: String = "",
    val ruc: String = "",
    val status: String? = GuideStatus.PENDING_REVIEW,
    val extractedData: Map<String, String> = emptyMap()
){
    fun isValid(): Boolean {
        val rucValido = ruc.length == 11 && ruc.all { it.isDigit() }
        val datosBasicos = guideName.isNotBlank() && date.isNotBlank()
        val integridadAprobacion = if (status == GuideStatus.APPROVED) {
            val productos = extractedData["productosJson"]
            !productos.isNullOrBlank() && productos != "[]"
        } else {
            true
        }

        return rucValido && datosBasicos && integridadAprobacion
    }
}

