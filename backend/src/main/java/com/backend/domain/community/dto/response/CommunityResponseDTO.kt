import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.repository.entity.Repositories
import java.time.LocalDateTime

data class CommunityResponseDTO(
    val userName: String,
    val userImage: String?,
    val repositoryName: String,
    val repositoryId: Long,
    val summary: String,
    val description: String?,
    val language: MutableList<String>,
    val totalScore: Int,
    val createDate: LocalDateTime,
    val publicStatus: Boolean,
    val htmlUrl: String
) {
    constructor(repositories: Repositories, analysis: AnalysisResult, score: Score) : this(

        // 프로퍼티 접근으로 수정
        userName = repositories.user.name,
        userImage = repositories.user.imageUrl,

        repositoryName = repositories.name,
        repositoryId = repositories.id!!,

        summary = analysis.summary,
        description = repositories.description,

        language = repositories.languages
            .map { it.language.name }
            .toMutableList(),

        totalScore = score.totalScore,
        createDate = analysis.createDate,

        publicStatus = repositories.isPublic,
        htmlUrl = repositories.htmlUrl
    )
}
