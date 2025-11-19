package com.backend.domain.evaluation.service

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.analysis.repository.ScoreRepository
import com.backend.domain.evaluation.dto.AiDto
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule  // ★ 이거 import 추가


class EvaluationServiceUnitTest {

    private val aiService = mockk<AiService>()
    private val repositoryJpaRepository = mockk<RepositoryJpaRepository>()
    private val analysisResultRepository = mockk<AnalysisResultRepository>()
    private val scoreRepository = mockk<ScoreRepository>()
    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(
            KotlinModule.Builder().build()    // ★ Kotlin data class 지원
        )
        .registerModule(JavaTimeModule())      // LocalDateTime 지원
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)



    private val evaluationService = EvaluationService(
        aiService = aiService,
        objectMapper = objectMapper,
        repositoryJpaRepository = repositoryJpaRepository,
        analysisResultRepository = analysisResultRepository,
        scoreRepository = scoreRepository
    )

//    @Test
//    fun `evaluateAndSave - 정상 JSON이면 AnalysisResult와 Score가 저장된다`() {
//        // given
//        val repoUrl = "https://github.com/test/repo"
//        val userId = 1L
//
//        val data = RepositoryData()
//        data.repositoryUrl = repoUrl   // RepositoryData 에 getRepositoryUrl()이 있다고 가정
//
//        val repo = mockk<Repositories>()
//
//        // ★ 여기! Optional 이 아니라 nullable 리턴으로 맞춰줌
//        every { repositoryJpaRepository.findByHtmlUrlAndUserId(repoUrl, userId) } returns repo
//
//        val json = """
//            {
//              "summary": "요약 내용",
//              "strengths": ["A", "B"],
//              "improvements": ["C"],
//              "scores": { "readme": 21, "test": 18, "commit": 20, "cicd": 17 }
//            }
//        """.trimIndent()
//
//        every { aiService.complete(any()) } returns AiDto.CompleteResponse(json)
//
//        val savedAnalysis = mockk<AnalysisResult>(relaxed = true)
//
//        every { savedAnalysis.id } returns 1L
//
//        val analysisSlot = slot<AnalysisResult>()
//        every { analysisResultRepository.save(capture(analysisSlot)) } returns savedAnalysis
//
//        val scoreSlot = slot<Score>()
//        every { scoreRepository.save(capture(scoreSlot)) } answers { scoreSlot.captured }
//
//        // when
//        val id = evaluationService.evaluateAndSave(data, userId)
//
//        // then
//        assertThat(id).isEqualTo(1L)
//
//        val ar = analysisSlot.captured
//        assertThat(ar.repositories).isEqualTo(repo)
//        assertThat(ar.summary).isEqualTo("요약 내용")
//        assertThat(ar.strengths).contains("- A")
//        assertThat(ar.strengths).contains("- B")
//        assertThat(ar.improvements).contains("- C")
//
//        val sc = scoreSlot.captured
//        assertThat(sc.analysisResult).isEqualTo(savedAnalysis)
//        assertThat(sc.readmeScore).isEqualTo(21)
//        assertThat(sc.testScore).isEqualTo(18)
//        assertThat(sc.commitScore).isEqualTo(20)
//        assertThat(sc.cicdScore).isEqualTo(17)
//    }

    @Test
    fun `evaluateAndSave - 저장소가 없으면 GITHUB_REPO_NOT_FOUND BusinessException`() {
        // given
        val repoUrl = "https://github.com/test/repo"
        val userId = 1L

        val data = RepositoryData()
        data.repositoryUrl = repoUrl

        // ★ 없을 때는 null 반환
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(repoUrl, userId) } returns null

        every { aiService.complete(any()) } returns AiDto.CompleteResponse(
            """{"summary":"x","strengths":[],"improvements":[],"scores":{"readme":0,"test":0,"commit":0,"cicd":0}}"""
        )

        // when
        val ex = assertThrows<BusinessException> {
            evaluationService.evaluateAndSave(data, userId)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(ErrorCode.GITHUB_REPO_NOT_FOUND)
        verify(exactly = 0) { analysisResultRepository.save(any()) }
        verify(exactly = 0) { scoreRepository.save(any()) }
    }

    @Test
    fun `callAiAndParse 내부 JSON 파싱 실패시 INTERNAL_ERROR BusinessException`() {
        // given
        val repoUrl = "https://github.com/test/repo"
        val userId = 1L

        val data = RepositoryData()
        data.repositoryUrl = repoUrl

        val repo = mockk<Repositories>()
        every { repositoryJpaRepository.findByHtmlUrlAndUserId(repoUrl, userId) } returns repo

        // 잘못된 JSON 문자열 (파싱 실패하도록)
        every { aiService.complete(any()) } returns AiDto.CompleteResponse("THIS IS NOT JSON")

        // when
        val ex = assertThrows<BusinessException> {
            evaluationService.evaluateAndSave(data, userId)
        }

        // then
        assertThat(ex.errorCode).isEqualTo(ErrorCode.INTERNAL_ERROR)
        verify(exactly = 0) { analysisResultRepository.save(any()) }
        verify(exactly = 0) { scoreRepository.save(any()) }
    }
}
