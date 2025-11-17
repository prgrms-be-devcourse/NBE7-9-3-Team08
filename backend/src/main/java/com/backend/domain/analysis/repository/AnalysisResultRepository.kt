package com.backend.domain.analysis.repository

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.repository.entity.Repositories
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnalysisResultRepository : JpaRepository<AnalysisResult, Long> {
    fun findAnalysisResultByRepositoriesIdOrderByCreateDateDesc(repositoryId: Long): List<AnalysisResult>
    fun countByRepositoriesId(repositoryId: Long): Long
    fun findByRepositoriesId(repositoryId: Long): List<AnalysisResult>
    fun findTopByRepositoriesIdOrderByCreateDateDesc(repositoryId: Long): AnalysisResult?
    fun findTopByRepositoriesOrderByCreateDateDesc(repositories: Repositories): AnalysisResult?
}
