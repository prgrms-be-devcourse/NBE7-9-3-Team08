package com.backend.domain.analysis.controller

import com.backend.domain.analysis.dto.request.AnalysisRequest
import com.backend.domain.analysis.dto.response.AnalysisResultResponseDto
import com.backend.domain.analysis.dto.response.AnalysisStartResponse
import com.backend.domain.analysis.dto.response.HistoryResponseDto
import com.backend.domain.analysis.service.AnalysisProgressService
import com.backend.domain.analysis.service.AnalysisService
import com.backend.domain.repository.dto.response.RepositoryComparisonResponse
import com.backend.domain.repository.dto.response.RepositoryResponse
import com.backend.domain.repository.service.RepositoryService
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.backend.global.response.ApiResponse
import com.backend.global.response.ApiResponse.Companion.success
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/analysis")
class AnalysisController(
    private val analysisService: AnalysisService,
    private val repositoryService: RepositoryService,
    private val analysisProgressService: AnalysisProgressService
) {
    // POST: 분석 요청
    @PostMapping
    fun analyzeRepository(
        @RequestBody request: AnalysisRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<AnalysisStartResponse>> {

        val repositoryId = analysisService.analyze(request.githubUrl, httpRequest)
            ?: throw BusinessException(ErrorCode.ANALYSIS_FAIL)

        val response = AnalysisStartResponse(repositoryId)
        return ResponseEntity.ok(success(response))
    }

    // GET: 사용자의 모든 Repository 목록 조회
    @GetMapping("/repositories")
    @Transactional(readOnly = true)
    fun getMemberHistory(
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<RepositoryResponse>>> {

        val repositories = repositoryService.getUserRepositories(httpRequest)
        return ResponseEntity.ok(success(repositories))
    }

    // GET: 특정 Repository의 분석 히스토리 조회
    @GetMapping("/repositories/{repositoriesId}")
    @Transactional(readOnly = true)
    fun getAnalysisByRepositoriesId(
        @PathVariable repositoriesId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<HistoryResponseDto>> {

        val response = analysisService.getHistory(repositoriesId, httpRequest)
        return ResponseEntity.ok(success(response))
    }

    // GET: 특정 분석 결과 상세 조회
    @GetMapping("/repositories/{repositoryId}/results/{analysisId}")
    @Transactional(readOnly = true)
    fun getAnalysisDetail(
        @PathVariable repositoryId: Long,
        @PathVariable analysisId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<AnalysisResultResponseDto>> {

        val response = analysisService.getAnalysisDetail(repositoryId, analysisId, httpRequest)
        return ResponseEntity.ok(success(response))
    }

    // DELETE: Repository 삭제
    @DeleteMapping("/{userId}/repositories/{repositoriesId}")
    fun deleteRepository(
        @PathVariable repositoriesId: Long,
        @PathVariable userId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<Void>> {

        analysisService.delete(repositoriesId, userId, httpRequest)
        return ResponseEntity.ok(success())
    }

    // DELETE: 특정 분석 결과 삭제
    @DeleteMapping("/{userId}/repositories/{repositoryId}/results/{analysisId}")
    fun deleteAnalysisResult(
        @PathVariable userId: Long,
        @PathVariable repositoryId: Long,
        @PathVariable analysisId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<Void>> {
        analysisService.deleteAnalysisResult(analysisId, repositoryId, userId, httpRequest)
        return ResponseEntity.ok(success())
    }

    // PUT: 분석 결과 공개 여부 변경
    @PutMapping("/{userId}/repositories/{repositoryId}/public")
    fun updatePublicStatus(
        @PathVariable userId: Long,
        @PathVariable repositoryId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<Void>> {
        analysisService.updatePublicStatus(repositoryId, userId, httpRequest)
        return ResponseEntity.ok(success())
    }

    // GET: 분석 현황 SSE
    @GetMapping("/stream/{userId}")
    fun stream(
        @PathVariable userId: Long,
        httpRequest: HttpServletRequest
    ): SseEmitter {
        return analysisProgressService.connect(userId, httpRequest)
    }

    // GET: 비교 기능용 Repository 목록 조회
    @GetMapping("/comparison")
    @Transactional(readOnly = true)
    fun getRepositoriesForComparison(
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<List<RepositoryComparisonResponse>>> {
        val repositories = analysisService.getRepositoriesForComparison(httpRequest)
        return ResponseEntity.ok(success(repositories))
    }
}
