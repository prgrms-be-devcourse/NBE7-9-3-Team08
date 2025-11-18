package com.backend.domain.analysis.controller

import com.backend.domain.analysis.dto.response.AnalysisResultResponseDto
import com.backend.domain.analysis.dto.response.HistoryResponseDto
import com.backend.domain.analysis.service.AnalysisProgressService
import com.backend.domain.analysis.service.AnalysisService
import com.backend.domain.repository.dto.response.RepositoryComparisonResponse
import com.backend.domain.repository.dto.response.RepositoryResponse
import com.backend.domain.repository.service.RepositoryService
import com.backend.domain.user.util.JwtUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AnalysisControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {

    @MockkBean lateinit var analysisService: AnalysisService
    @MockkBean lateinit var repositoryService: RepositoryService
    @MockkBean lateinit var analysisProgressService: AnalysisProgressService
    @MockkBean lateinit var jwtUtil: JwtUtil

    @BeforeEach
    fun setup() {
        every { jwtUtil.getUserId(any<HttpServletRequest>()) } returns 1L

        every { analysisService.delete(any(), any(), any()) } returns Unit
        every { analysisService.updatePublicStatus(any(), any(), any()) } returns mockk()
        every { analysisService.deleteAnalysisResult(any(), any(), any(), any()) } returns Unit
    }


    @Test
    @DisplayName("POST /api/analysis - 분석 요청 성공")
    fun `분석 요청 성공`() {
        val url = "https://github.com/test/repo"

        every { analysisService.analyze(url, any()) } returns 123L

        val body = """
            { "githubUrl": "$url" }
        """.trimIndent()

        mockMvc.perform(
            post("/api/analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.repositoryId").value(123L))
    }

    @Test
    @DisplayName("GET /api/analysis/repositories - 사용자 저장소 목록 조회")
    fun `사용자 저장소 목록 조회`() {

        val repo = RepositoryResponse(
            id = 1L,
            name = "repo1",
            description = "my repo",
            htmlUrl = "https://github.com/test/repo1",
            publicRepository = true,
            mainBranch = "main",
            languages = listOf("Java"),
            createDate = LocalDateTime.now(),
            ownerId = 1L
        )

        every { repositoryService.getUserRepositories(any()) } returns listOf(repo)

        mockMvc.perform(get("/api/analysis/repositories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("repo1"))
            .andExpect(jsonPath("$.data[0].htmlUrl").value("https://github.com/test/repo1"))
    }

    @Test
    @DisplayName("GET /api/analysis/repositories/{id} - 분석 버전 목록 조회")
    fun `특정 저장소 분석 버전 목록 조회`() {

        val repo = RepositoryResponse(
            id = 1L,
            name = "repo1",
            description = "desc",
            htmlUrl = "https://github.com/test/repo1",
            publicRepository = true,
            mainBranch = "main",
            languages = listOf("Java"),
            createDate = LocalDateTime.now(),
            ownerId = 1L
        )

        val version = HistoryResponseDto.AnalysisVersionDto(
            analysisId = 10L,
            analysisDate = LocalDateTime.now(),
            totalScore = 95,
            versionLabel = "v1 (2025-01-01)"
        )

        val dto = HistoryResponseDto(
            repository = repo,
            analysisVersions = listOf(version)
        )

        every { analysisService.getHistory(1L, any()) } returns dto

        mockMvc.perform(get("/api/analysis/repositories/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.repository.id").value(1L))
            .andExpect(jsonPath("$.data.analysisVersions.length()").value(1))
            .andExpect(jsonPath("$.data.analysisVersions[0].analysisId").value(10L))
    }

    @Test
    @DisplayName("GET /api/analysis/repositories/{repoId}/results/{analysisId} - 분석 상세 조회")
    fun `분석 상세 조회`() {

        val dto = AnalysisResultResponseDto(
            totalScore = 90,
            readmeScore = 95,
            testScore = 88,
            commitScore = 86,
            cicdScore = 92,
            summary = "summary ok",
            strengths = "strong parts",
            improvements = "weak parts",
            createDate = LocalDateTime.now()
        )

        every { analysisService.getAnalysisDetail(1L, 2L, any()) } returns dto

        mockMvc.perform(get("/api/analysis/repositories/1/results/2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalScore").value(90))
            .andExpect(jsonPath("$.data.readmeScore").value(95))
    }

    @Test
    @DisplayName("DELETE /api/analysis/{userId}/repositories/{repoId} - 저장소 삭제")
    fun `저장소 삭제`() {
        mockMvc.perform(delete("/api/analysis/1/repositories/2"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("DELETE /api/analysis/{userId}/repositories/{repoId}/results/{analysisId} - 분석 결과 삭제 성공")
    fun `분석 결과 삭제`() {
        mockMvc.perform(delete("/api/analysis/1/repositories/2/results/3"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("PUT /api/analysis/{userId}/repositories/{repoId}/public - 공개 여부 변경 성공")
    fun `공개 여부 변경`() {
        mockMvc.perform(put("/api/analysis/1/repositories/2/public"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    @DisplayName("GET /api/analysis/stream/{userId} - SSE 연결 성공")
    fun `SSE 연결 성공`() {
        every { analysisProgressService.connect(1L, any()) } returns SseEmitter(Long.MAX_VALUE)

        mockMvc.perform(get("/api/analysis/stream/1"))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk)
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /api/analysis/comparison - 비교 목록 조회")
    fun `비교 목록 조회`() {

        val comparison = RepositoryComparisonResponse(
            repositoryId = 1L,
            name = "repo1",
            htmlUrl = "https://github.com/test/repo1",
            languages = listOf("Java"),
            latestAnalysis = RepositoryComparisonResponse.AnalysisInfo(
                analysisId = 10L,
                analyzedAt = LocalDateTime.now(),
                scores = RepositoryComparisonResponse.CategoryScores(
                    readme = 95,
                    test = 90,
                    commit = 85,
                    cicd = 92,
                    total = 90
                )
            )
        )

        every { analysisService.getRepositoriesForComparison(any()) } returns listOf(comparison)

        mockMvc.perform(get("/api/analysis/comparison"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("repo1"))
            .andExpect(jsonPath("$.data[0].latestAnalysis.scores.total").value(90))
    }
}
