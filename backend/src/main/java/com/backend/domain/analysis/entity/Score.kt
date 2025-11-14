package com.backend.domain.analysis.entity

import jakarta.persistence.*

@Entity
@Table(name = "score")
open class Score() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_result_id", nullable = false)
    open lateinit var analysisResult: AnalysisResult
        protected set

    @Column(nullable = false)
    open var readmeScore: Int = 0
        protected set

    @Column(nullable = false)
    open var testScore: Int = 0
        protected set

    @Column(nullable = false)
    open var commitScore: Int = 0
        protected set

    @Column(nullable = false)
    open var cicdScore: Int = 0
        protected set

    // 종합 점수 (Java에서는 getTotalScore() 로 보임)
    @get:Transient
    open val totalScore: Int
        get() = readmeScore + testScore + commitScore + cicdScore

    /**
     * Java에서 @Builder가 타겟으로 잡던 생성자
     */
    constructor(
        analysisResult: AnalysisResult,
        readmeScore: Int,
        testScore: Int,
        commitScore: Int,
        cicdScore: Int,
    ) : this() {
        this.analysisResult = analysisResult
        // 양방향 연관관계 정합성 유지
        analysisResult.assignScore(this)

        this.readmeScore = readmeScore
        this.testScore = testScore
        this.commitScore = commitScore
        this.cicdScore = cicdScore
    }

    companion object {
        /**
         * Java 코드에서 쓰던 Score.builder() 패턴 그대로 지원
         */
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var analysisResult: AnalysisResult? = null
        private var readmeScore: Int = 0
        private var testScore: Int = 0
        private var commitScore: Int = 0
        private var cicdScore: Int = 0

        fun analysisResult(analysisResult: AnalysisResult) = apply { this.analysisResult = analysisResult }
        fun readmeScore(readmeScore: Int) = apply { this.readmeScore = readmeScore }
        fun testScore(testScore: Int) = apply { this.testScore = testScore }
        fun commitScore(commitScore: Int) = apply { this.commitScore = commitScore }
        fun cicdScore(cicdScore: Int) = apply { this.cicdScore = cicdScore }

        fun build(): Score {
            val ar = requireNotNull(analysisResult) { "analysisResult must not be null" }
            return Score(
                analysisResult = ar,
                readmeScore = readmeScore,
                testScore = testScore,
                commitScore = commitScore,
                cicdScore = cicdScore,
            )
        }
    }
}
