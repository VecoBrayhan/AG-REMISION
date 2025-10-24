package domain

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String, photoUrl: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun logout()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun deleteCurrentUser(): Result<Unit>
    suspend fun signInWithGoogle(idToken: String, accessToken: String): Result<Unit>
    suspend fun signOutGoogle(): Result<Unit>
}