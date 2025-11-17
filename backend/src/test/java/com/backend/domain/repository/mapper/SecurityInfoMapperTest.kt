package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.TreeResponse
import com.backend.domain.repository.dto.response.github.TreeResponse.TreeItem
import com.backend.domain.repository.service.mapper.SecurityInfoMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class SecurityInfoMapperTest {

    private val mapper = SecurityInfoMapper()

    @Test
    @DisplayName("TreeResponse가 null이면 기본값으로 처리")
    fun mapNullTree_shouldHandleGracefully() {
        val data = RepositoryData()

        mapper.mapSecurityInfo(data, null)

        assertThat(data.hasSensitiveFile).isFalse()
    }

    @Test
    @DisplayName(".env는 민감 파일이지만 .env.example은 안전 파일로 처리")
    fun mapEnvFiles_shouldDistinguishRealAndExample() {
        val data = RepositoryData()

        val tree = TreeResponse(
            tree = listOf(
                TreeItem(".env", "blob"),
                TreeItem(".env.example", "blob"),
                TreeItem(".env.local", "blob")
            ),
            truncated = false
        )

        mapper.mapSecurityInfo(data, tree)

        assertThat(data.sensitiveFilePaths)
            .contains(".env", ".env.local")
            .doesNotContain(".env.example")
    }

    @Test
    @DisplayName("secret.json은 탐지하지만 secret.json.example은 탐지하지 않아야 함")
    fun securityMapper_shouldDistinguishSensitiveAndSafeFiles() {
        val data = RepositoryData()

        val tree = TreeResponse(
            tree = listOf(
                TreeItem("config/secret.json", "blob"),
                TreeItem("config/secret.json.example", "blob")
            ),
            truncated = false
        )

        mapper.mapSecurityInfo(data, tree)

        assertThat(data.hasSensitiveFile).isTrue()
        assertThat(data.sensitiveFilePaths).contains("config/secret.json")
    }

    @Test
    @DisplayName("다양한 빌드 파일들이 올바르게 감지되어야 함")
    fun mapVariousBuildFiles_shouldBeDetected() {
        val data = RepositoryData()

        val tree = TreeResponse(
            tree = listOf(
                TreeItem("pom.xml", "blob"),
                TreeItem("build.gradle", "blob"),
                TreeItem("package.json", "blob"),
                TreeItem("requirements.txt", "blob")
            ),
            truncated = false
        )

        mapper.mapSecurityInfo(data, tree)

        assertThat(data.hasBuildFile).isTrue()
        assertThat(data.buildFiles).hasSize(4)
    }
}
