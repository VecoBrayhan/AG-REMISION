package domain

import kotlinx.serialization.Serializable

@Serializable
data class RemissionGuide(
    val id: String = "",
    val guideName: String = "",
    val date: String = "",
    val ruc: String = "",
    val status: String = "",
    val extractedData: Map<String, String> = emptyMap()
)