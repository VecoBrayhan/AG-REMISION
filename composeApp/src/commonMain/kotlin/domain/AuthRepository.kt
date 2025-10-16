package domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserId(): String?
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, password: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun logout()
}