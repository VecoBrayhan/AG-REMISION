package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import domain.AuthRepository
import domain.UserRepository
import domain.model.UserProfile

class FirestoreUserRepository(
    private val authRepository: AuthRepository
) : UserRepository {

    private val firestore = Firebase.firestore
    private val usersCollection get() = firestore.collection("users")

    override suspend fun getCurrentUser(): UserProfile? {
        val currentUserId = authRepository.getCurrentUserId() ?: return null
        return try {
            usersCollection.document(currentUserId).get().data<UserProfile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null || currentUserId != userProfile.uid) {
            return Result.failure(IllegalStateException("ID de usuario no coincide o no está logueado."))
        }
        return try {
            usersCollection.document(userProfile.uid).set(userProfile, merge = false)
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}