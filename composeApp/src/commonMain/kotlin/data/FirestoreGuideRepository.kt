package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import domain.GuideRepository
import domain.RemissionGuide
import kotlinx.coroutines.flow.flow

class FirestoreGuideRepository : GuideRepository {

    private val firestore = Firebase.firestore

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
}