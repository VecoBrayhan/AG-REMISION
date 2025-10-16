package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import domain.AuthRepository
import domain.model.UserProfile

class EmailNotVerifiedException(message: String) : Exception(message)
class FirebaseAuthRepository : AuthRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password)
            auth.currentUser?.reload()
            val isVerified = auth.currentUser?.isEmailVerified ?: false
            if (isVerified) {
                auth.currentUser?.uid?.let { uid ->
                    firestore.collection("users").document(uid).set(mapOf("isVerified" to true), merge = true)
                }
                Result.success(Unit)
            } else {
                Result.failure(EmailNotVerifiedException("Por favor, verifica tu correo electrónico para continuar."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val user = result.user
            user?.let {
                it.sendEmailVerification()
                val userProfile = UserProfile(
                    uid = it.uid,
                    name = name,
                    email = email,
                    isVerified = false
                )
                firestore.collection("users").document(it.uid).set(userProfile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            auth.currentUser?.updatePassword(newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }
}