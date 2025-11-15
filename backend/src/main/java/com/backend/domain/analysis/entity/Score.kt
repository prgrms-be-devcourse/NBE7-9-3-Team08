package com.backend.domain.analysis.entity

import com.backend.domain.repository.entity.Repositories
import jakarta.persistence.*

@Entity
@Table(name = "score")
open class Score protected constructor(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id", nullable = false)
    open var analysisResult: AnalysisResult,

    @Column(nullable = false)
    open var readmeScore: Int,

    @Column(nullable = false)
    open var testScore: Int,

    @Column(nullable = false)
    open var commitScore: Int,

    @Column(nullable = false)
    open var cicdScore: Int,
) {
    protected constructor() : this(
        analysisResult = AnalysisResult.create(
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
            improvements = ""
        ),
        readmeScore = 0,
        testScore = 0,
        commitScore = 0,
        cicdScore = 0
    )

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @get:Transient
    open val totalScore: Int
        get() = readmeScore + testScore + commitScore + cicdScore

    init {
        analysisResult.assignScore(this)
    }

    companion object {
        @JvmStatic
        fun create(
            analysisResult: AnalysisResult,
            readmeScore: Int,
            testScore: Int,
            commitScore: Int,
            cicdScore: Int,
        ): Score {
            return Score(
                analysisResult = analysisResult,
                readmeScore = readmeScore,
                testScore = testScore,
                commitScore = commitScore,
                cicdScore = cicdScore,
            )
        }
    }
}