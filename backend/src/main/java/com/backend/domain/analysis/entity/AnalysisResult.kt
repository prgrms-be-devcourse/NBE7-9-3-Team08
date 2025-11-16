package com.backend.domain.analysis.entity

import com.backend.domain.community.entity.Comment
import com.backend.domain.repository.entity.Repositories
import jakarta.persistence.*
import lombok.Getter
import java.time.LocalDateTime

@Entity
@Table(name = "analysis_result")
open class AnalysisResult() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    open lateinit var repositories: Repositories
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    open lateinit var summary: String
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    open lateinit var strengths: String
        protected set

    @Column(nullable = false, columnDefinition = "TEXT")
    open lateinit var improvements: String
        protected set

    @Column(nullable = false, name = "createData")
    open lateinit var createDate: LocalDateTime
        protected set

    @OneToOne(
        mappedBy = "analysisResult",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true,
    )
    open var score: Score? = null
        protected set

    @OneToMany(
        mappedBy = "analysisResult",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
    )
    open val comments: MutableList<Comment> = mutableListOf()

    /**
     * Java @Builder가 타겟으로 잡던 생성자
     * new AnalysisResult(repositories, summary, strengths, improvements, createDate)
     */
    constructor(
        repositories: Repositories,
        summary: String,
        strengths: String,
        improvements: String,
        createDate: LocalDateTime,
    ) : this() {
        this.repositories = repositories
        this.summary = summary
        this.strengths = strengths
        this.improvements = improvements
        this.createDate = createDate
    }

    fun assignScore(score: Score) {
        this.score = score
    }

    /**
     * Java 코드의 AnalysisResult.builder() 패턴을 그대로 지원하기 위한 Builder 구현
     */
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var repositories: Repositories? = null
        private var summary: String? = null
        private var strengths: String? = null
        private var improvements: String? = null
        private var createDate: LocalDateTime? = null

        fun repositories(repositories: Repositories) = apply { this.repositories = repositories }
        fun summary(summary: String) = apply { this.summary = summary }
        fun strengths(strengths: String) = apply { this.strengths = strengths }
        fun improvements(improvements: String) = apply { this.improvements = improvements }
        fun createDate(createDate: LocalDateTime) = apply { this.createDate = createDate }

        fun build(): AnalysisResult {
            val repo = requireNotNull(repositories) { "repositories must not be null" }
            val s = requireNotNull(summary) { "summary must not be null" }
            val st = requireNotNull(strengths) { "strengths must not be null" }
            val imp = requireNotNull(improvements) { "improvements must not be null" }
            val cd = requireNotNull(createDate) { "createDate must not be null" }

            return AnalysisResult(
                repositories = repo,
                summary = s,
                strengths = st,
                improvements = imp,
                createDate = cd,
            )
        }
    }
}
