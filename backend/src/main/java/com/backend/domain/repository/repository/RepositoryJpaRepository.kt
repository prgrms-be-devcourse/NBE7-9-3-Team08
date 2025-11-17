package com.backend.domain.repository.repository

import com.backend.domain.repository.entity.Repositories
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RepositoryJpaRepository : JpaRepository<Repositories, Long> {
    fun findByHtmlUrl(htmlUrl: String): Optional<Repositories>
    fun findByHtmlUrlAndUserId(htmlUrl: String, userId: Long): Optional<Repositories>
    fun findByUserId(userId: Long): List<Repositories>
    fun findByPublicRepository(publicRepository: Boolean): List<Repositories>
    fun findByPublicRepositoryTrue(pageable: Pageable): Page<Repositories>
}
