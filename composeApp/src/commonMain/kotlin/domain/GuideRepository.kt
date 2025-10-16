package domain
import kotlinx.coroutines.flow.Flow

interface GuideRepository {
    fun getGuides(): Flow<List<RemissionGuide>>
    suspend fun addGuide(guide: RemissionGuide): Result<Unit>
}