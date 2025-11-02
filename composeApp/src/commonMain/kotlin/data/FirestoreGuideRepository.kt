package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference // Import specific type
import dev.gitlive.firebase.firestore.firestore
// Importa las Data Classes del modelo de respuesta de Gemini
import domain.model.GeminiResponse
import domain.model.Producto // Asegúrate que esta clase esté en domain.model también
import domain.GuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.flow.Flow // Import Flow explicitly
import kotlinx.coroutines.flow.catch // Import catch explicitly
import kotlinx.coroutines.flow.flow // Import flow explicitly
import kotlinx.coroutines.flow.map // Import map explicitly
import utils.encodeBase64 // Tu función expect/actual

// --- Ktor y kotlinx.serialization imports ---
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException // Para el log de error específico
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerializationException // Para capturar errores de kotlinx.serialization
import kotlinx.serialization.encodeToString // Para convertir la lista de productos a String
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
// ------------------------------------------

class FirestoreGuideRepository : GuideRepository {

    private val firestore = Firebase.firestore
    // Referencia explícita a la colección para claridad
    private val guidesCollection: CollectionReference = firestore.collection("guides")

    // Instancia de Json configurada para ser tolerante
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }

    // Cliente HTTP Ktor configurado
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
        // install(HttpTimeout) { requestTimeoutMillis = 60000 } // Opcional
    }

    // --- getGuides() CORREGIDO CON ASIGNACIÓN MANUAL DE ID ---
    override fun getGuides(): Flow<List<RemissionGuide>> {
        println("Firestore: Suscribiéndose a snapshots de 'guides'")
        return guidesCollection.snapshots // Obtiene el Flow<QuerySnapshot>
            .map { querySnapshot -> // Transforma cada QuerySnapshot
                println("Firestore: Recibidos ${querySnapshot.documents.size} documentos.")
                querySnapshot.documents.mapNotNull { documentSnapshot -> // Procesa cada DocumentSnapshot
                    try {
                        // 1. Deserializa los datos del documento (el ID será el por defecto: "")
                        val guideData = documentSnapshot.data<RemissionGuide>()
                        // 2. Obtiene el ID REAL del documento
                        val actualId = documentSnapshot.id
                        // 3. Crea una copia con el ID correcto
                        val guideWithId = guideData.copy(id = actualId)

                        // 4. VERIFICA que el ID asignado no esté vacío (importante)
                        if (guideWithId.id.isNotBlank()) {
                            guideWithId // Devuelve la guía con el ID correcto si no está vacío
                        } else {
                            println(" Error Crítico: Documento ${documentSnapshot.id} resultó en RemissionGuide con ID vacío después de .copy()")
                            null // Descarta si el ID sigue vacío por alguna razón inesperada
                        }
                    } catch (e: Exception) {
                        println(" Error al deserializar documento ${documentSnapshot.id}: ${e.message}")
                        null // Descarta documentos malformados
                    }
                }.also { guides ->
                    // Log opcional para ver los IDs finales antes de emitir
                    println("Firestore: Emitiendo ${guides.size} guías válidas con IDs: ${guides.map { it.id }}")
                }
            }
            .catch { e -> // Maneja errores en el Flow
                println(" Error en el Flow de getGuides: ${e.message}")
                emit(emptyList()) // Emite lista vacía en caso de error
            }
    }


    // --- addGuide() corregido usando add() ---
    override suspend fun addGuide(guide: RemissionGuide): Result<Unit> {
        return try {
            if (guide.id.isNotBlank()) {
                // --- Actualizar Documento Existente ---
                val docRef = guidesCollection.document(guide.id)
                docRef.set(guide)
                println(" Guía actualizada en Firestore con ID: ${guide.id}")
            } else {
                // --- Crear Nuevo Documento con ID Automático ---
                // add() guarda el objeto y Firestore genera el ID.
                // IMPORTANTE: El objeto 'guide' que pasamos aquí NO tendrá el ID todavía.
                val docRef = guidesCollection.add(guide)
                println(" Nueva guía guardada en Firestore con ID generado: ${docRef.id}")
                // Si necesitaras actualizar el objeto en memoria con el ID, tendrías que hacer:
                // docRef.set(guide.copy(id = docRef.id)) // Opcional, solo si necesitas el ID en el objeto guardado
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println(" Error al guardar en Firestore: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }


    // --- extractDataFromGuide() ---
    override suspend fun extractDataFromGuide(fileBytes: ByteArray, fileName: String): Result<RemissionGuide> {
        // Tu URL de producción de Vercel
        val apiUrl = "https://backend-ag-remision.vercel.app/api/extractGuideData"
        println(" KMP -> Vercel: Llamando a $apiUrl con archivo: $fileName (Tamaño: ${fileBytes.size} bytes)")

        return try {
            val requestBody = mapOf(
                "fileBase64" to fileBytes.encodeBase64(),
                "fileName" to fileName
            )

            val httpResponse: HttpResponse = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Primero, lee el cuerpo como texto para logging
            val responseBodyText = httpResponse.bodyAsText()

            if (httpResponse.status.isSuccess()) {
                println(" KMP <- Vercel: Respuesta OK (Texto): $responseBodyText")

                // Ahora, intenta deserializar el texto a las Data Classes
                val geminiResponse: GeminiResponse = jsonParser.decodeFromString(responseBodyText)
                println(" KMP <- Vercel: Respuesta OK deserializada: $geminiResponse")

                val extractedDetails = geminiResponse.extractedData
                val extractedDataStringMap = mutableMapOf<String, String>()

                // Mapeo seguro de campos simples
                extractedDetails?.fechaLlegada?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["fechaLlegada"] = it }
                extractedDetails?.fechaRegistro?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["fechaRegistro"] = it }
                extractedDetails?.costoTransporte?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["costoTransporte"] = it }

                // Mapeo de la lista de productos a String JSON
                extractedDetails?.productos?.let { listaProductos ->
                    if (listaProductos.isNotEmpty()) {
                        try {
                            // Serializa la List<Producto> a un String JSON
                            extractedDataStringMap["productosJson"] = jsonParser.encodeToString(listaProductos)
                            println(" KMP: Lista de productos serializada a JSON string.")
                        } catch (e: SerializationException) {
                            println(" KMP: Error al serializar lista de productos a JSON: ${e.message}")
                            extractedDataStringMap["productosError"] = "Error al procesar lista de productos (serialización)"
                        } catch (e: Exception){
                            println(" KMP: Error inesperado al serializar lista de productos: ${e.message}")
                            extractedDataStringMap["productosError"] = "Error inesperado al procesar productos"
                        }
                    } else {
                        println(" KMP: La lista de productos recibida estaba vacía.")
                    }
                } ?: println(" KMP: No se encontró la clave 'productos' en extractedData.")


                val guide = RemissionGuide(
                    // ID se deja en blanco, será asignado por Firestore al usar add()
                    guideName = fileName,
                    date = geminiResponse.date?.takeIf { it.isNotBlank() } ?: "N/A",
                    ruc = geminiResponse.ruc?.takeIf { it.isNotBlank() } ?: "N/A",
                    status = "Extraído",
                    extractedData = extractedDataStringMap
                )
                println(" KMP: Objeto RemissionGuide creado: $guide")
                Result.success(guide)

            } else {
                // El backend devolvió un error HTTP (4xx o 5xx)
                println(" KMP <- Vercel: Respuesta de ERROR (${httpResponse.status}): $responseBodyText")
                val errorMessage = try {
                    // Intenta parsear el JSON de error del backend
                    val jsonElement = jsonParser.parseToJsonElement(responseBodyText)
                    jsonElement.jsonObject["error"]?.jsonPrimitive?.content ?: "Error desconocido del backend (${httpResponse.status})"
                } catch (parseError: Exception) {
                    // Si la respuesta de error no es JSON válido
                    "Error inesperado del backend (código ${httpResponse.status})"
                }
                Result.failure(Exception(errorMessage))
            }

        } catch (e: JsonConvertException) {
            // Error específico de Ktor al intentar deserializar una respuesta 2xx que NO coincide con GeminiResponse
            println(" KMP x Vercel: ERROR de Ktor al deserializar la respuesta OK: ${e.message}")
            // Intenta obtener el texto que causó el problema (puede estar truncado en el mensaje)
            val problematicJson = e.message?.substringAfter("JSON input: ") ?: "(No se pudo extraer el JSON del error)"
            println(" KMP x Vercel: JSON recibido que causó el error de deserialización: $problematicJson")
            e.printStackTrace()
            Result.failure(Exception("Error al interpretar la respuesta del servidor (formato inesperado).", e))
        } catch (e: Exception) {
            // Otros errores (red, timeouts, etc.)
            println(" KMP x Vercel: ERROR general en la llamada Ktor: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

