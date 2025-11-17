package com.backend.domain.repository.dto.response

import com.backend.domain.repository.entity.Repositories
import java.time.LocalDateTime

/**
 * Repository 기본 정보 응답 DTO
 * 사용자의 Repository 목록 조회 시 사용
 */
data class RepositoryResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val publicRepository: Boolean,
    val mainBranch: String,
    val languages: List<String>,
    val createDate: LocalDateTime,
    val ownerId: Long
) {
    companion object {
        @JvmStatic
        fun from(entity: Repositories): RepositoryResponse {

            val languages = entity.getLanguages()
                .map { it.language.name }

            return RepositoryResponse(
                id = entity.id!!,
                name = entity.name,
                description = entity.description,
                htmlUrl = entity.htmlUrl,
                publicRepository = entity.publicRepository,
                mainBranch = entity.mainBranch,
                languages = languages,
                createDate = entity.createDate!!,
                ownerId = entity.user?.id!!
            )
        }
    }
}
