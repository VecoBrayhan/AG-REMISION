package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.firestore
import domain.model.GeminiResponse
import domain.GuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import utils.encodeBase64
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FirestoreGuideRepository(
    private val authRepository: FirebaseAuthRepository
) : GuideRepository {

    private val firestore = Firebase.firestore

    private fun getGuidesCollection(userId: String): CollectionReference {
        if (userId.isBlank()) {
            throw IllegalArgumentException("El ID de usuario no puede estar vacío.")
        }
        return firestore.collection("users").document(userId).collection("guides")
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        prettyPrint = false
    }
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(jsonParser)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getGuides(): Flow<List<RemissionGuide>> {
        println("Firestore: Suscribiéndose al flujo de autenticación")

        // 2. Usar flatMapLatest para re-suscribirse cuando el usuario cambie
        return authRepository.currentUser.flatMapLatest { user ->
            if (user == null || user.uid.isBlank()) {
                // Si no hay usuario, devuelve un flujo con una lista vacía
                println("Firestore: No hay usuario logueado. Emmiting lista vacía.")
                flowOf(emptyList())
            } else {
                // Si hay usuario, obtiene su colección de guías
                val userId = user.uid
                println("Firestore: Usuario $userId logueado. Suscribiéndose a 'users/$userId/guides'")
                getGuidesCollection(userId).snapshots
                    .map { querySnapshot ->
                        println("Firestore: Recibidos ${querySnapshot.documents.size} documentos para $userId.")
                        querySnapshot.documents.mapNotNull { documentSnapshot ->
                            try {
                                val guideData = documentSnapshot.data<RemissionGuide>()
                                val actualId = documentSnapshot.id
                                val guideWithId = guideData.copy(id = actualId)
                                if (guideWithId.id.isNotBlank()) {
                                    guideWithId
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                println("Error al parsear documento: ${documentSnapshot.id}. Error: ${e.message}")
                                null
                            }
                        }
                    }
                    .catch { e ->
                        println(" Error al obtener guías de Firestore para $userId: ${e.message}")
                        emit(emptyList())
                    }
            }
        }
    }

    override suspend fun addGuide(guide: RemissionGuide): Result<Unit> {
        // 3. Obtener el userId actual de forma síncrona
        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            return Result.failure(Exception("No hay usuario autenticado para guardar la guía."))
        }

        return try {
            val collection = getGuidesCollection(userId)
            if (guide.id.isNotBlank()) {
                val docRef = collection.document(guide.id)
                docRef.set(guide)
            } else {
                val docRef = collection.add(guide)
                println(" Nueva guía guardada en Firestore 'users/$userId/guides' con ID generado: ${docRef.id}")
                docRef.set(guide.copy(id = docRef.id))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            println(" Error al guardar en Firestore para $userId: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteGuide(guideId: String): Result<Unit> {
        // 4. Obtener el userId actual de forma síncrona
        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            return Result.failure(Exception("No hay usuario autenticado para eliminar la guía."))
        }

        return try {
            if (guideId.isBlank()) {
                return Result.failure(IllegalArgumentException("El ID de la guía no puede estar vacío."))
            }
            val docRef = getGuidesCollection(userId).document(guideId)
            docRef.delete()
            println(" Guía eliminada de Firestore: 'users/$userId/guides/$guideId'")
            Result.success(Unit)
        } catch (e: Exception) {
            println(" Error al eliminar en Firestore para $userId: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }


    /**
     * Sin cambios: Este método no interactúa con las colecciones de Firestore.
     */
    override suspend fun extractDataFromGuide(fileBytes: ByteArray, fileName: String): Result<RemissionGuide> {
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
            val responseBodyText = httpResponse.bodyAsText()
            if (httpResponse.status.isSuccess()) {
                println(" KMP <- Vercel: Respuesta OK (Texto): $responseBodyText")
                val geminiResponse: GeminiResponse = jsonParser.decodeFromString(responseBodyText)
                println(" KMP <- Vercel: Respuesta OK deserializada: $geminiResponse")
                val extractedDetails = geminiResponse.extractedData
                val extractedDataStringMap = mutableMapOf<String, String>()
                extractedDetails?.fechaLlegada?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["fechaLlegada"] = it }
                extractedDetails?.fechaRegistro?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["fechaRegistro"] = it }
                extractedDetails?.costoTransporte?.takeIf { it.isNotBlank() }?.let { extractedDataStringMap["costoTransporte"] = it }
                extractedDetails?.productos?.let { listaProductos ->
                    if (listaProductos.isNotEmpty()) {
                        try {
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
                    guideName = fileName,
                    date = geminiResponse.date?.takeIf { it.isNotBlank() } ?: "N/A",
                    ruc = geminiResponse.ruc?.takeIf { it.isNotBlank() } ?: "N/A",
                    status = "Extraído",
                    extractedData = extractedDataStringMap
                )
                println(" KMP: Objeto RemissionGuide creado: $guide")
                Result.success(guide)

            } else {
                println(" KMP <- Vercel: Respuesta de ERROR (${httpResponse.status}): $responseBodyText")
                val errorMessage = try {
                    val jsonElement = jsonParser.parseToJsonElement(responseBodyText)
                    jsonElement.jsonObject["error"]?.jsonPrimitive?.content ?: "Error desconocido del backend (${httpResponse.status})"
                } catch (parseError: Exception) {
                    "Error inesperado del backend (código ${httpResponse.status})"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: JsonConvertException) {
            println(" KMP x Vercel: ERROR de Ktor al deserializar la respuesta OK: ${e.message}")
            val problematicJson = e.message?.substringAfter("JSON input: ") ?: "(No se pudo extraer el JSON del error)"
            println(" KMP x Vercel: JSON recibido que causó el error de deserialización: $problematicJson")
            e.printStackTrace()
            Result.failure(Exception("Error al interpretar la respuesta del servidor (formato inesperado).", e))
        } catch (e: Exception) {
            println(" KMP x Vercel: ERROR general en la llamada Ktor: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}