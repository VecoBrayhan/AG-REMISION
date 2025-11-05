package domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
@Serializable
data class Producto(
    @SerialName("descripcion")
    val descripcion: String? = null,
    @SerialName("cantidad")
    val cantidad: Double? = null,
    @SerialName("unidad")
    val unidad: String? = null
)

@Serializable
data class ExtractedDetails(
    @SerialName("fechaLegada")
    val fechaLlegada: String? = null,
    @SerialName("fechaRegistro")
    val fechaRegistro: String? = null,
    @SerialName("costoTransporte")
    val costoTransporte: String? = null,
    @SerialName("productos")
    val productos: List<Producto>? = null
)
@Serializable
data class GeminiResponse(
    @SerialName("date")
    val date: String? = null,
    @SerialName("ruc")
    val ruc: String? = null,
    @SerialName("extractedData")
    val extractedData: ExtractedDetails? = null
)