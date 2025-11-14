package domain

import kotlinx.serialization.Serializable

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
)