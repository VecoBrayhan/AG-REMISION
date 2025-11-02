package domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName // <-- ¡Asegúrate de importar esto!

/** Representa un producto extraído por la IA */
@Serializable
data class Producto(
    // Mapea la clave "item" (del JSON) a la variable "descripcion" (de Kotlin)
    @SerialName("item")
    val descripcion: String? = null,

    @SerialName("cantidad")
    val cantidad: Double? = null,

    @SerialName("unidad")
    val unidad: String? = null
)

/** Representa la estructura anidada 'extractedData' */
@Serializable
data class ExtractedDetails(
    // Mapea la clave "fechaLegada" (del JSON) a la variable "fechaLlegada" (de Kotlin)
    @SerialName("fechaLegada")
    val fechaLlegada: String? = null,

    @SerialName("fechaRegistro")
    val fechaRegistro: String? = null,

    @SerialName("costoTransporte")
    val costoTransporte: String? = null,

    @SerialName("productos")
    val productos: List<Producto>? = null
)

/** Representa la respuesta JSON completa del backend */
@Serializable
data class GeminiResponse(
    @SerialName("date")
    val date: String? = null,

    @SerialName("ruc")
    val ruc: String? = null,

    @SerialName("extractedData")
    val extractedData: ExtractedDetails? = null
)
