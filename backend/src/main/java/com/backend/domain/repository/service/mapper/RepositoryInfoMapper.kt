package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.RepoResponse
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class RepositoryInfoMapper {

    private val kst: ZoneId = ZoneId.of("Asia/Seoul")

    // ResponseData 메타 정보 [기본 정보]
    fun mapBasicInfo(data: RepositoryData, response: RepoResponse) {
        data.repositoryName = response.fullName ?: ""
        data.repositoryUrl = response.htmlUrl ?: ""
        data.description = response.description
        data.primaryLanguage = response.language
        data.repositoryCreatedAt = response.createdAt
            ?.atZoneSameInstant(kst)
            ?.toLocalDateTime()
            ?: LocalDateTime.now()
    }
}
