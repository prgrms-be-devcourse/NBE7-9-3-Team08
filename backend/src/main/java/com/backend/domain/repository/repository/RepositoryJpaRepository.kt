package com.backend.domain.repository.repository

import com.backend.domain.repository.entity.Repositories
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RepositoryJpaRepository : JpaRepository<Repositories, Long> {
    fun findByHtmlUrlAndUserId(htmlUrl: String, userId: Long): Repositories?
    fun findByUserId(userId: Long): List<Repositories>
    fun findByPublicRepository(publicRepository: Boolean): List<Repositories>


    /* === 레포지토리 조회 === */
    // 전체 레포지토리 조회 - 최신순
    @Query("""
    SELECT r FROM Repositories r
    JOIN r.analysisResults ar
    WHERE r.publicRepository = true
    ORDER BY ar.createDate DESC
""")
    fun findAllOrderByLatestAnalysis(): List<Repositories>

    // 전체 레포지토리 조회 - 점수순
    @Query("""
    SELECT r FROM Repositories r
    JOIN r.analysisResults ar
    JOIN ar.score s
    WHERE r.publicRepository = true
    ORDER BY s.totalScore DESC
""")
    fun findAllOrderByScoreDesc(): List<Repositories>

    // 검색 레포지토리 조회 - 레포지토리 이름으로 검색
    fun findByNameContainingIgnoreCaseAndPublicRepositoryTrue(
        content: String,
        pageable: Pageable
    ): Page<Repositories>

    // 검색 레포지토리 조회 - 유저 이름으로 검색
    fun findByUser_NameContainingIgnoreCaseAndPublicRepositoryTrue(
        contnet: String,
        pageable: Pageable
    ): Page<Repositories>

    /* 히스토리 검색 레포지토리 조회 */

    // 레포지토리 이름으로 검색
    fun findByUser_IdAndNameContainingIgnoreCase(
        userId: Long,
        content: String,
        pageable: Pageable
    ): Page<Repositories>


    @Query(
        value = """
            SELECT *
            FROM repositories r
            WHERE r.html_url = :url
              AND r.user_id = :userId
        """,
        nativeQuery = true
    )
    fun findIncludingDeleted(
        @Param("url") url: String,
        @Param("userId") userId: Long
    ): Repositories?
}
