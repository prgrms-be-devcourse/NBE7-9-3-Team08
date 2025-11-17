package com.backend.domain.analysis.service

import com.backend.domain.analysis.dto.response.AnalysisResultResponseDto
import com.backend.domain.analysis.dto.response.HistoryResponseDto
import com.backend.domain.analysis.dto.response.HistoryResponseDto.AnalysisVersionDto
import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.lock.RedisLockManager
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.evaluation.service.EvaluationService
import com.backend.domain.repository.dto.response.RepositoryComparisonResponse
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.RepositoryResponse
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.service.RepositoryService
import com.backend.domain.user.util.JwtUtil
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnalysisService(
    private val repositoryService: RepositoryService,
    private val analysisResultRepository: AnalysisResultRepository,
    private val evaluationService: EvaluationService,
    private val repositoryJpaRepository: RepositoryJpaRepository,
    private val sseProgressNotifier: SseProgressNotifier,
    private val lockManager: RedisLockManager,
    private val jwtUtil: JwtUtil,
) {

    companion object {
        private val log = LoggerFactory.getLogger(AnalysisService::class.java)
    }

    /**
     * Analysis 분석 프로세스 오케스트레이션 담당
     * 1. GitHub URL 파싱 및 검증
     * 2. Repository 도메인을 통한 데이터 수집
     * 3. Evaluation 도메인을 통한 AI 평가
     * 4. 분석 결과 저장
     */
    @Transactional
    fun analyze(githubUrl: String, request: HttpServletRequest): Long {
        val userId = jwtUtil.getUserId(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        val (owner, repo) = parseGitHubUrl(githubUrl)
        val cacheKey = "$userId:$githubUrl"

        if (!lockManager.tryLock(cacheKey)) {
            throw BusinessException(ErrorCode.ANALYSIS_IN_PROGRESS)
        }

        try {
            safeSendSse(userId, "status", "분석 시작")

            // 1. Repository 데이터 수집
            val repositoryData: RepositoryData = try {
                val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)
                lockManager.refreshLock(cacheKey)
                safeSendSse(userId, "status", "GitHub 데이터 수집 완료")
                log.info("Repository Data 수집 완료: {}", data)
                data
            } catch (e: BusinessException) {
                log.error("Repository 데이터 수집 실패: {}/{}", owner, repo, e)
                safeSendSse(userId, "error", "Repository 데이터 수집 실패")
                throw e
            }

            // 2. 저장된 Repository 조회
            val savedRepository = repositoryJpaRepository
                .findByHtmlUrlAndUserId(repositoryData.repositoryUrl, userId)
                ?: throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)

            val repositoryId = savedRepository.id
                ?: throw IllegalStateException("Repository id is null")

            // 3. OpenAI API 데이터 분석 및 저장
            try {
                evaluationService.evaluateAndSave(repositoryData, userId)
                lockManager.refreshLock(cacheKey)
                safeSendSse(userId, "status", "AI 평가 완료")
            } catch (e: BusinessException) {
                safeSendSse(userId, "error", "AI 평가 실패: ${e.message}")
                throw BusinessException(ErrorCode.ANALYSIS_FAIL)
            }

            safeSendSse(userId, "complete", "최종 리포트 생성")
            return repositoryId
        } finally {
            try {
                lockManager.releaseLock(cacheKey)
                log.info("분석 락 해제: cacheKey={}", cacheKey)
            } catch (e: Exception) {
                log.warn("⚠️ 락 해제 중 예외 발생 (무시됨): {}", e.message)
            }
        }
    }

    // SSE 전송 헬퍼 메서드
    private fun safeSendSse(userId: Long, event: String, message: String) {
        try {
            sseProgressNotifier.notify(userId, event, message)
        } catch (e: Exception) {
            log.warn(
                "SSE 전송 실패 (분석은 계속): userId={}, event={}, error={}",
                userId, event, e.message
            )
        }
    }

    // 특정 Repository의 모든 분석 결과 조회 (최신순)
    fun getAnalysisResultList(repositoryId: Long): List<AnalysisResult> {
        return analysisResultRepository
            .findAnalysisResultByRepositoriesIdOrderByCreateDateDesc(repositoryId)
    }

    // 분석 결과 ID로 단건 조회
    fun getAnalysisById(analysisId: Long): AnalysisResult {
        return analysisResultRepository.findById(analysisId)
            .orElseThrow { BusinessException(ErrorCode.ANALYSIS_NOT_FOUND) }
    }

    // 분석 히스토리 조회
    @Transactional(readOnly = true)
    fun getHistory(repositoryId: Long, request: HttpServletRequest): HistoryResponseDto {
        // 1. Repository 조회
        val repository = repositoryJpaRepository.findById(repositoryId)
            .orElseThrow { BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND) }

        // 2. 권한 검증 (서비스에서 처리)
        val requestUserId = jwtUtil.getUserId(request) // null 가능 (비로그인)
        validateAccess(repository, requestUserId)

        // 3. 분석 결과 조회 (최신순)
        val analysisResults =
            analysisResultRepository.findAnalysisResultByRepositoriesIdOrderByCreateDateDesc(repositoryId)

        // 4. DTO 변환 (버전 번호는 최신 분석이 가장 큰 숫자)
        val versions: List<HistoryResponseDto.AnalysisVersionDto> =
            analysisResults.mapIndexed { index, analysis ->
                val versionNumber = analysisResults.size - index
                AnalysisVersionDto.from(analysis, versionNumber)
            }

        val repositoryResponse = RepositoryResponse.from(repository)
        return HistoryResponseDto.of(repositoryResponse, versions)
    }

    // 분석 상세 조회
    @Transactional(readOnly = true)
    fun getAnalysisDetail(
        repositoryId: Long?,
        analysisId: Long,
        request: HttpServletRequest
    ): AnalysisResultResponseDto {
        // 1. 분석 결과 조회
        val analysisResult = analysisResultRepository.findById(analysisId)
            .orElseThrow { BusinessException(ErrorCode.ANALYSIS_NOT_FOUND) }

        // 2. repositoryId 일치 검증
        if (analysisResult.repositories.id != repositoryId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }

        // 3. 권한 검증
        val requestUserId = jwtUtil.getUserId(request) // null 가능 (비로그인)
        validateAccess(analysisResult.repositories, requestUserId)

        val score = analysisResult.score
            ?: throw IllegalStateException("AnalysisResult.score is null")

        return AnalysisResultResponseDto.from(analysisResult, score)
    }

    // Repository 삭제
    @Transactional
    fun delete(repositoriesId: Long, userId: Long?, request: HttpServletRequest) {
        val requestUserId = jwtUtil.getUserId(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (requestUserId != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val targetRepository = repositoryJpaRepository.findById(repositoriesId)
            .orElseThrow { BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND) }

        if (targetRepository.user?.id != userId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        repositoryJpaRepository.delete(targetRepository)
    }

    // 특정 분석 결과 삭제
    @Transactional
    fun deleteAnalysisResult(
        analysisResultId: Long,
        repositoryId: Long?,
        memberId: Long?,
        request: HttpServletRequest
    ) {
        val requestUserId = jwtUtil.getUserId(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (requestUserId != memberId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val analysisResult = analysisResultRepository.findById(analysisResultId)
            .orElseThrow { BusinessException(ErrorCode.ANALYSIS_NOT_FOUND) }

        if (analysisResult.repositories.id != repositoryId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE)
        }

        if (analysisResult.repositories.user?.id != memberId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        analysisResult.deleted = true
    }

    // 분석 결과 공개 여부 변경
    @Transactional
    fun updatePublicStatus(repositoryId: Long, memberId: Long?, request: HttpServletRequest): Repositories {
        val requestUserId = jwtUtil.getUserId(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (requestUserId != memberId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val repository = repositoryJpaRepository.findById(repositoryId)
            .orElseThrow { BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND) }

        if (repository.user?.id != memberId) {
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        val newStatus = !repository.publicRepository

        // 공개로 변경하는 경우, 분석 결과가 최소 1개 이상 존재해야 함
        if (newStatus) {
            val analysisCount = analysisResultRepository.countByRepositoriesId(repositoryId)
            if (analysisCount == 0L) {
                throw BusinessException(ErrorCode.ANALYSIS_NOT_FOUND)
            }
        }

        repository.updatePublicStatus(newStatus)
        return repository
    }

    // 리포지토리 접근 권한 검증
    private fun validateAccess(repository: Repositories, requestUserId: Long?) {
        // 1. 공개 리포지토리는 누구나 접근 가능
        if (repository.publicRepository) {
            log.debug(
                "공개 리포지토리 접근: repoId={}, requestUserId={}",
                repository.id, requestUserId
            )
            return
        }

        // 2. 비공개 리포지토리는 로그인 필수
        if (requestUserId == null) {
            log.warn(
                "비로그인 사용자의 비공개 리포지토리 접근 시도: repoId={}",
                repository.id
            )
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        }

        // 3. 비공개 리포지토리는 소유자만 접근 가능
        val ownerId = repository.user?.id
        if (requestUserId != ownerId) {
            log.warn(
                "권한 없는 사용자의 비공개 리포지토리 접근: requestUserId={}, ownerId={}, repoId={}",
                requestUserId, ownerId, repository.id
            )
            throw BusinessException(ErrorCode.FORBIDDEN)
        }

        log.debug(
            "비공개 리포지토리 소유자 접근: userId={}, repoId={}",
            requestUserId, repository.id
        )
    }

    // 리포지토리 Url 파싱
    private fun parseGitHubUrl(githubUrl: String): Array<String> {
        log.info("분석 요청 url: {}", githubUrl)

        if (!githubUrl.startsWith("https://github.com/")) {
            throw BusinessException(ErrorCode.INVALID_GITHUB_URL)
        }

        var path = githubUrl.removePrefix("https://github.com/").trim()
        if (path.endsWith("/")) {
            path = path.dropLast(1)
        }

        val parts = path.split("/")
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw BusinessException(ErrorCode.INVALID_REPOSITORY_PATH)
        }

        return arrayOf(parts[0].trim(), parts[1].trim())
    }

    // 비교 기능, 사용자의 모든 Repository와 최신 분석 점수 조회
    @Transactional(readOnly = true)
    fun getRepositoriesForComparison(request: HttpServletRequest): List<RepositoryComparisonResponse> {
        val userId = jwtUtil.getUserId(request) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        val repositories = repositoryJpaRepository.findByUserId(userId) ?: mutableListOf()

        return repositories
            .filterNotNull()
            .mapNotNull { repo ->
                val repoId = repo.id ?: return@mapNotNull null
                val latestAnalysis =
                    analysisResultRepository.findTopByRepositoriesIdOrderByCreateDateDesc(repoId)

                latestAnalysis?.let { analysis ->
                    RepositoryComparisonResponse.from(repo, analysis)
                }
            }
    }
}
