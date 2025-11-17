package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.CommitResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Component
class CommitInfoMapper {

    // ResponseData 유지보수성 [커밋 관련]
    fun mapCommitInfo(data: RepositoryData, response: List<CommitResponse>?) {
        if (response.isNullOrEmpty()) {
            setDefaultValues(data)
            return
        }

        // 마지막 커밋 시점
        val commit = response[0].commit
        if (commit == null) {
            setDefaultValues(data)
            return
        }

        val lastCommitDate = commit.author?.date?.let { parseCommitDate(it) } ?: LocalDateTime.now()
        data.lastCommitDate = lastCommitDate

        // 마지막 커밋 이후 경과일
        data.daysSinceLastCommit = calculateDaysSinceLastCommit(lastCommitDate)

        // 최근 90일 커밋 수
        data.commitCountLast90Days = response.size

        // 최근 10개 커밋 메시지
        data.recentCommits = extractRecentCommitMessages(response)
    }

    private fun setDefaultValues(data: RepositoryData) {
        data.lastCommitDate = null
        data.daysSinceLastCommit = 0
        data.commitCountLast90Days = 0
        data.recentCommits = emptyList()
    }

    private fun parseCommitDate(date: String?): LocalDateTime {
        if (date.isNullOrBlank()) {
            return LocalDateTime.now()
        }

        return try {
            val utc = ZonedDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME)
            utc.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        } catch (e: DateTimeParseException) {
            LocalDateTime.now()
        }
    }

    private fun calculateDaysSinceLastCommit(date: LocalDateTime?): Int {
        if (date == null) return 0
        val daysBetween = ChronoUnit.DAYS.between(date, LocalDateTime.now())
        return max(0, daysBetween.toInt())
    }

    private fun extractRecentCommitMessages(commitResponses: List<CommitResponse>): List<RepositoryData.CommitInfo> {
        return commitResponses
            .filterNotNull()
            .take(10)
            .map { createCommitInfoFromMessage(it) }
    }

    private fun createCommitInfoFromMessage(commitResponse: CommitResponse): RepositoryData.CommitInfo {
        val commitInfo = RepositoryData.CommitInfo()

        val commit = commitResponse.commit

        // 메시지 처리
        val finalMessage = commit?.message
            ?.trim()
            ?.let { cleanCommitMessage(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: "No commit message"

        commitInfo.message = finalMessage

        // 커밋 날짜
        commit?.author?.date
            ?.let { parseCommitDate(it) }
            ?.let { commitInfo.committedDate = it }

        return commitInfo
    }

    private fun cleanCommitMessage(message: String): String {
        val firstLine = message.split("\n").first().trim()

        return if (firstLine.length > 100) {
            firstLine.substring(0, 97) + "..."
        } else {
            firstLine
        }
    }
}
