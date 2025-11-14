package domain

import domain.model.UserProfile

interface UserRepository {
    suspend fun getCurrentUser(): UserProfile?
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
}