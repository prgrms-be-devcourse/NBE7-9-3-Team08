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

    @Async
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
            val repoData = repositoryService.fetchAndSaveRepository(owner, repo, userId)
            safeSendSse(userId, "status", "GitHub 데이터 수집 완료")
            lockManager.refreshLock(cacheKey)
            log.info("RepositoryData 수집 완료: {}", repoData)

            // 2. 저장된 Repository 조회
            val saved = repositoryJpaRepository.findByHtmlUrlAndUserId(githubUrl, userId)
                ?: throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)

            val repositoryId = saved.id
                ?: throw IllegalStateException("Repository id null")

            // 3. OpenAI API 데이터 분석 및 저장
            evaluationService.evaluateAndSave(repoData, userId)
            safeSendSse(userId, "status", "AI 평가 완료")
            lockManager.refreshLock(cacheKey)

            // 4. 완료 전송
            safeSendSse(userId, "complete", "최종 리포트 작성")
        } catch (e: Exception) {
            log.error("비동기 분석 중 오류: userId={}, url={} / {}", userId, githubUrl, e.message)
            safeSendSse(userId, "error", "분석 처리 중 오류 발생")
        } finally {
            lockManager.releaseLock(cacheKey)
            log.info("분석 락 해제: {}", cacheKey)
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