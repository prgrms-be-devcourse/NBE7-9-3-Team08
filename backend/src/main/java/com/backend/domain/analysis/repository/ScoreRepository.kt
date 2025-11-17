package com.backend.domain.analysis.repository

import com.backend.domain.analysis.entity.Score
import org.springframework.data.jpa.repository.JpaRepository

interface ScoreRepository : JpaRepository<Score, Long>
