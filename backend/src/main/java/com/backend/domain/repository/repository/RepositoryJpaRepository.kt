package com.backend.domain.repository.repository

import com.backend.domain.repository.entity.Repositories
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RepositoryJpaRepository : JpaRepository<Repositories, Long> {
    fun findByHtmlUrlAndUserId(htmlUrl: String, userId: Long): Repositories?
    fun findByUserId(userId: Long): List<Repositories>
    fun findByPublicRepository(publicRepository: Boolean): List<Repositories>
    fun findByPublicRepositoryTrue(pageable: Pageable): Page<Repositories>
}
