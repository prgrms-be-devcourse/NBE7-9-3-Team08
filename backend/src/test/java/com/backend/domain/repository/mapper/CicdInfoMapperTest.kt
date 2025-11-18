package com.backend.domain.repository.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.TreeResponse
import com.backend.domain.repository.service.mapper.CicdInfoMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class CicdInfoMapperTest {

    private val mapper = CicdInfoMapper()

    @Test
    @DisplayName("TreeResponse가 null이어도 NPE 없이 처리")
    fun `TreeResponse null → NPE 없이 처리`() {
        val data = RepositoryData()

        mapper.mapCicdInfo(data, null)

        assertThat(data.hasCICD).isFalse()
    }

    @Test
    @DisplayName("다양한 CI/CD 설정 파일들이 감지되어야 함")
    fun `다양한 CI-CD 파일 감지`() {
        val data = RepositoryData()
        val tree = TreeResponse(
            tree = listOf(
                TreeResponse.TreeItem(".github/workflows/ci.yml", "blob"),
                TreeResponse.TreeItem(".gitlab-ci.yml", "blob"),
                TreeResponse.TreeItem("Jenkinsfile", "blob"),
                TreeResponse.TreeItem(".circleci/config.yml", "blob")
            ),
            truncated = false
        )

        mapper.mapCicdInfo(data, tree)

        assertThat(data.hasCICD).isTrue()
        assertThat(data.cicdFiles).hasSize(4)
    }

    @Test
    @DisplayName("DockerFile(대소문자 혼합)도 감지되어야 함")
    fun `DockerFile 대소문자 무시하고 감지`() {
        val data = RepositoryData()
        val tree = TreeResponse(
            tree = listOf(
                TreeResponse.TreeItem(
                    path = "infra/DockerFile",
                    type = "blob"
                )
            ),
            truncated = false
        )

        mapper.mapCicdInfo(data, tree)

        assertThat(data.hasDockerfile).isTrue()
    }

}
