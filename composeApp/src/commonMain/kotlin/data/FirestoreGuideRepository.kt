package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import domain.GuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.flow.flow
import utils.encodeBase64


// --- IMPORTACIONES DE KTOR ---
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
class FirestoreGuideRepository : GuideRepository {

    // --- INICIALIZA KTOR ---
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Muy importante
                isLenient = true
            })
        }
    }

    private val firestore = Firebase.firestore
    private val functions = Firebase.functions
    override fun getGuides() = flow {
        firestore.collection("guides").snapshots.collect { querySnapshot ->
            val guides = querySnapshot.documents.map { documentSnapshot ->
                documentSnapshot.data<RemissionGuide>()
            }
            emit(guides)
        }
    }

    override suspend fun addGuide(guide: RemissionGuide): Result<Unit> {
        return try {
            val documentId = firestore.collection("guides").document.id
            firestore.collection("guides")
                .document(documentId)
                .set(guide.copy(id = documentId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractDataFromGuide(fileBytes: ByteArray, fileName: String): Result<RemissionGuide> {
        return try {
            // Preparamos los datos para la Cloud Function
            val data = mapOf(
                "fileBase64" to fileBytes.encodeBase64(), // Enviar como string Base64
                "fileName" to fileName
            )

            // Llamar a la Cloud Function por su nombre (ej. "extractGuideData")
            // 'data' será el objeto JSON resultante
            val result = functions.httpsCallable("extractGuideData")(data)

            // Firebase Functions (GitLive) devuelve un 'Map' que debemos
            // convertir a nuestro data class 'RemissionGuide'
            val dataMap = result.data() as Map<String, Any?>

            val guide = RemissionGuide(
                guideName = fileName,
                date = dataMap["date"] as? String ?: "",
                ruc = dataMap["ruc"] as? String ?: "",
                status = "Extraído", // Poner un estado
                // La IA nos devolverá un mapa con los datos variables
                extractedData = dataMap["extractedData"] as? Map<String, String> ?: emptyMap()
            )

            Result.success(guide)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}