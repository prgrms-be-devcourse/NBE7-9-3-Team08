package com.backend.domain.analysis.entity

import com.backend.domain.community.entity.Comment
import com.backend.domain.repository.entity.Repositories
import jakarta.persistence.*
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.Where
import java.time.LocalDateTime

@Entity
@Table(name = "analysis_result")
@SQLDelete(sql = "UPDATE analysis_result SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
open class AnalysisResult protected constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    open var repositories: Repositories,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var summary: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var strengths: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var improvements: String,

    @Column(nullable = false, name = "create_date")
    open var createDate: LocalDateTime,

    @Column(nullable = false)
    var deleted: Boolean = false
) {
    protected constructor() : this(
        repositories = Repositories.create(
            user = com.backend.domain.user.entity.User(),
            name = "",
            description = null,
            htmlUrl = "",
            publicRepository = false,
            mainBranch = "main"
        ),
        summary = "",
        strengths = "",
        improvements = "",
        createDate = LocalDateTime.now()
    )

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null  // protected set 제거

    @OneToOne(
        mappedBy = "analysisResult",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY
    )
    open var score: Score? = null
        protected set

    @OneToMany(
        mappedBy = "analysisResult",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY
    )
    open val comments: MutableList<Comment> = mutableListOf()

    fun assignScore(score: Score) {
        if (this.score == null) {
            this.score = score
        }
    }

    companion object {
        @JvmStatic
        fun create(
            repositories: Repositories,
            summary: String,
            strengths: String,
            improvements: String,
            createDate: LocalDateTime = LocalDateTime.now(),
        ): AnalysisResult {
            return AnalysisResult(
                repositories = repositories,
                summary = summary,
                strengths = strengths,
                improvements = improvements,
                createDate = createDate,
            )
        }
    }
}