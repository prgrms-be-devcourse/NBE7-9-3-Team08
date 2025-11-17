package com.backend.domain.analysis.service

import com.backend.domain.analysis.lock.RedisLockManager
import com.backend.domain.evaluation.service.EvaluationService
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.repository.service.RepositoryService
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AnalysisAnalyzeService (
    private val repositoryService: RepositoryService,
    private val evaluationService: EvaluationService,
    private val repositoryJpaRepository: RepositoryJpaRepository,
    private val sseProgressNotifier: SseProgressNotifier,
    private val lockManager: RedisLockManager
) {

    private val log = LoggerFactory.getLogger(AnalysisAnalyzeService::class.java)

    @Async("analysisExecutor")
    fun runAnalysisAsync(
        userId: Long,
        githubUrl: String,
        owner: String,
        repo: String,
        cacheKey: String
    ) {
        try {
            safeSendSse(userId, "status", "분석 시작")

            // 1. Repository 데이터 수집
            val repositoryData = try {
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
                log.error("AI 평가 실패: userId={}, url={}", userId, githubUrl, e)
                safeSendSse(userId, "error", "AI 평가 실패: ${e.message}")
                throw BusinessException(ErrorCode.ANALYSIS_FAIL)
            }

            // 4. 완료 전송
            safeSendSse(userId, "complete", "최종 리포트 작성")
        } catch (e: Exception) {
            log.error("비동기 분석 중 오류: userId={}, url={} / {}", userId, githubUrl, e.message)
            safeSendSse(userId, "error", "분석 처리 중 오류 발생")
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
}