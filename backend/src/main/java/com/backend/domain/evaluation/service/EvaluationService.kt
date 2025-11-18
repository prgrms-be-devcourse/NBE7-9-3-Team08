package com.backend.domain.evaluation.service

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.analysis.repository.ScoreRepository
import com.backend.domain.evaluation.dto.AiDto
import com.backend.domain.evaluation.dto.EvaluationDto.AiResult
import com.backend.domain.evaluation.dto.EvaluationDto.Scores
import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.regex.Pattern

@Service
class EvaluationService(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val repositoryJpaRepository: RepositoryJpaRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val scoreRepository: ScoreRepository,
) {

    private val log = LoggerFactory.getLogger(EvaluationService::class.java)

    /**
     * RepositoryData + userId 를 받아서
     * 1) OpenAI로 품질 평가를 요청하고
     * 2) AnalysisResult / Score 엔티티를 저장한 뒤
     * 3) 저장된 AnalysisResult의 id 를 반환한다.
     */
    @Transactional
    fun evaluateAndSave(data: RepositoryData, userId: Long): Long {
        val ai: AiResult = callAiAndParse(data)

        val url = data.repositoryUrl
        if (url.isBlank()) {
            log.error("repositoryUrl is null or blank in RepositoryData")
            throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)
        }

        val repo = repositoryJpaRepository
            .findByHtmlUrlAndUserId(url, userId)
            ?: throw BusinessException(ErrorCode.GITHUB_REPO_NOT_FOUND)


        // 팩토리 메서드 사용
        val analysis = AnalysisResult.create(
            repositories = repo,
            summary = safe(ai.summary),
            strengths = joinBullets(ai.strengths),
            improvements = joinBullets(ai.improvements),
            createDate = LocalDateTime.now(),
        )

        // AnalysisResult 먼저 저장
        val saved = analysisResultRepository.save(analysis)

        val sc: Scores = ai.scores

        // 팩토리 메서드 사용
        val score = Score.create(
            analysisResult = saved,
            readmeScore = sc.readme,
            testScore = sc.test,
            commitScore = sc.commit,
            cicdScore = sc.cicd,
        )

        scoreRepository.save(score)

        val analysisResultId = saved.id
            ?: throw IllegalStateException("AnalysisResult.id is null after save")

        log.info("✅ Evaluation saved. analysisResultId={}", analysisResultId)

        return analysisResultId
    }

    /**
     * OpenAI API 호출 + 응답(JSON) → AiResult DTO 파싱
     */
    fun callAiAndParse(data: RepositoryData): AiResult {
        return try {
            val content = objectMapper.writeValueAsString(data)

            val prompt = """
                You are a senior software engineering reviewer.
                Analyze the given GitHub repository data (JSON) and return ONLY a valid JSON.
                No commentary, no code fences.
            
                Scoring: total up to 100 (README 0~30, TEST 0~30, COMMIT 0~25, CICD 0~15).
            
                HARD RULES (MUST FOLLOW):
                - Use ONLY integers for all scores.
                - The sum of scores (readme + test + commit + cicd) MUST NOT exceed 100.
                - If "hasCICD" is false in the input JSON, then scores.cicd MUST be between 0 and 5 (usually 0).
                - If "hasTestDirectory" is false, then scores.test MUST be 0.
                - If "testCoverageRatio" < 0.3, then scores.test MUST be <= 15.
                - If the "improvements" array has length >= 1, then the total score MUST be <= 95.
                - NEVER return a total score of 100 unless the repository is essentially perfect
                  (no major improvements to mention).
            
                JSON schema:
                {
                  "summary": "one-paragraph summary in Korean",
                  "strengths": ["...","..."],
                  "improvements": ["...","..."],
                  "scores": {
                    "readme": 0,
                    "test": 0,
                    "commit": 0,
                    "cicd": 0
                  }
                }
            """.trimIndent()


            val res: AiDto.CompleteResponse =
                aiService.complete(AiDto.CompleteRequest(content, prompt))

            val raw = res.result
            val json = extractJson(raw)

            objectMapper.readValue(json, AiResult::class.java)
        } catch (e: Exception) {
            log.error("AI evaluation failed", e)
            throw BusinessException(ErrorCode.INTERNAL_ERROR)
        }
    }

    /**
     * OpenAI 응답에서 ```json 코드블럭 제거 + 가장 바깥 JSON 객체만 추출
     */
    private fun extractJson(text: String?): String {
        require(text != null) { "AI result is null" }

        val cleaned = text.replace("```json", "```").trim()
        val matcher = Pattern.compile("\\{.*}", Pattern.DOTALL).matcher(cleaned)

        return if (matcher.find()) matcher.group() else cleaned
    }

    /**
     * 리스트를 "- bullet" 형식의 멀티라인 문자열로 변환
     */
    private fun joinBullets(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""

        return list
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n") { "- $it" }
    }

    private fun safe(s: String?): String =
        s?.trim() ?: ""

}
