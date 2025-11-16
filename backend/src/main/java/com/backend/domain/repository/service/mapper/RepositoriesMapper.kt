package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.github.RepoResponse
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.user.entity.User
import org.springframework.stereotype.Component

@Component
class RepositoriesMapper {
    // 처음 분석하는 리포지토리의 경우 기본 메타 데이터 저장
    fun toEntity(response: RepoResponse, user: User): Repositories {
        return Repositories.create(
            user = user,
            name = response.name ?: "unknown",
            description = response.description,
            htmlUrl = response.htmlUrl ?: "unknown",
            publicRepository = false,
            mainBranch = response.defaultBranch ?: "main",
            languages = emptyList()
        )
    }
}
