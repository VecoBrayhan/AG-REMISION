package data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
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

    override suspend fun register(name: String, email: String, password: String, photoUrl: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password)
            val user = result.user
            user?.let {
                it.sendEmailVerification()
                val userProfile = UserProfile(
                    uid = it.uid,
                    name = name,
                    email = email,
                    photoUrl = photoUrl,
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

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(
                IllegalStateException("No hay usuario logueado para eliminar.")
            )
            val firestore = Firebase.firestore
            val collections = listOf("RespuestasEncuesta", "Terrenos", "users")
            for (col in collections) {
                val querySnapshot = when (col) {
                    "users" -> {
                        val byField = firestore.collection(col).where("id", userId).get().documents
                        byField.ifEmpty { listOfNotNull(firestore.collection(col).document(userId).get()) }
                    }
                    else -> firestore.collection(col).where("userId", userId).get().documents
                }
                if (querySnapshot.isNotEmpty()) {
                    val batch = firestore.batch()
                    for (doc in querySnapshot) {
                        batch.delete(doc.reference)
                    }
                    batch.commit()
                }
            }
            auth.currentUser?.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // En FirebaseAuthRepository.kt

    // En FirebaseAuthRepository.kt

    override suspend fun signInWithGoogle(idToken: String, accessToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.credential(idToken = idToken, accessToken = accessToken)
            val result = auth.signInWithCredential(credential)

            result.user?.let { user ->
                // --- LÓGICA MODIFICADA ---
                // 1. Referencia al documento del usuario en Firestore
                val userDocRef = firestore.collection("users").document(user.uid)

                // 2. Comprueba si el documento YA EXISTE
                val userDoc = userDocRef.get()

                // 3. SOLO escribe los datos de Google SI el documento NO existe (primera vez)
                if (!userDoc.exists) {
                    val userProfile = UserProfile(
                        uid = user.uid,
                        name = user.displayName ?: "Usuario Google",
                        email = user.email ?: "",
                        photoUrl = user.photoURL ?: "", // Asegúrate que tu UserProfile tenga este campo
                        isVerified = true // El correo de Google siempre está verificado
                    )
                    // Usamos set() normal aquí porque sabemos que no existe
                    userDocRef.set(userProfile)
                } else {
                    // Opcional: Si el documento existe, podrías actualizar solo 'isVerified' si fuera necesario,
                    // pero generalmente no hace falta ya que si existe, probablemente ya está verificado.
                    // userDocRef.set(mapOf("isVerified" to true), merge = true)
                    println("Usuario ${user.uid} ya existe en Firestore. No se sobrescriben datos desde Google.")
                }
                // -------------------------
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOutGoogle(): Result<Unit> = signOutGooglePlatform()

}

expect suspend fun signOutGooglePlatform(): Result<Unit>
