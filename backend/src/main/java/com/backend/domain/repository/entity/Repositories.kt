package com.backend.domain.repository.entity

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.repository.dto.response.github.RepoResponse
import com.backend.domain.repository.util.LanguageUtils
import com.backend.domain.user.entity.User
import com.backend.global.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where

@Entity
@Table(
    name = "repositories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_html_url_user",
            columnNames = ["html_url", "user_id"]
        )
    ]
)
@SQLDelete(sql = "UPDATE repositories SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
class Repositories protected constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(nullable = false)
    var htmlUrl: String,

    @Column(name = "public_repository")
    var publicRepository: Boolean = false,

    @Column(name = "main_branch")
    var mainBranch: String,

    @Column(nullable = false)
    var deleted: Boolean = false,

    initLanguages: List<RepositoryLanguage> = emptyList()
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(
        mappedBy = "repositories",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val analysisResults: MutableList<AnalysisResult> = mutableListOf()

    @OneToMany(
        mappedBy = "repositories",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val languages: MutableList<RepositoryLanguage> = mutableListOf()

    /** 언어 목록 조회 (외부에서는 읽기만 가능) */
    fun getLanguages(): List<RepositoryLanguage> = languages.toList()

    init {
        initLanguages.forEach { addLanguage(it) }
    }

    /** 언어 추가 */
    fun addLanguage(language: RepositoryLanguage) {
        languages.add(language)
        language.repositories = this
    }

    /** GitHub API 응답 기반 업데이트 */
    fun updateFrom(repoInfo: RepoResponse) {
        name = repoInfo.name ?: name
        description = repoInfo.description
        mainBranch = repoInfo.defaultBranch ?: mainBranch
    }

    /** Public 여부 수정 */
    fun updatePublicStatus(isPublic: Boolean) {
        this.publicRepository = isPublic
    }

    /** GitHub languages 업데이트 */
    fun updateLanguagesFrom(newLanguagesData: Map<String, Int>) {
        val newLanguages = newLanguagesData.keys
            .map { LanguageUtils.fromGitHubName(it) }
            .toSet()

        val existingLanguages = languages
            .map { it.language }
            .toSet()

        if (newLanguages == existingLanguages) return

        languages.removeIf { it.language !in newLanguages }

        val toAdd = newLanguages - existingLanguages

        toAdd.forEach { lang ->
            addLanguage(
                RepositoryLanguage(
                    repositories = this,
                    language = lang
                )
            )
        }
    }

    companion object {
        @JvmStatic
        fun create(
            user: User,
            name: String,
            description: String?,
            htmlUrl: String,
            publicRepository: Boolean,
            mainBranch: String,
            languages: List<RepositoryLanguage> = emptyList()
        ): Repositories {
            return Repositories(
                user = user,
                name = name,
                description = description,
                htmlUrl = htmlUrl,
                publicRepository = publicRepository,
                mainBranch = mainBranch,
                initLanguages = languages
            )
        }
    }
}
