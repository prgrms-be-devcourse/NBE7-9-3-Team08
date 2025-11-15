package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.TreeResponse
import org.springframework.stereotype.Component

@Component
class CicdInfoMapper {

    fun mapCicdInfo(data: RepositoryData, response: TreeResponse?) {
        if (response == null || response.tree.isNullOrEmpty()) {
            setEmptyCicdData(data)
            return
        }

        val filePaths = extractFilePaths(response)

        // CI/CD 파일
        val cicdFiles = findCicdFiles(filePaths)
        data.hasCICD = cicdFiles.isNotEmpty()
        data.cicdFiles = cicdFiles

        // Build 파일
        val buildFiles = findBuildFiles(filePaths)
        data.hasBuildFile = buildFiles.isNotEmpty()
        data.buildFiles = buildFiles

        // Dockerfile
        data.hasDockerfile = hasDockerFiles(filePaths)
    }

    private fun setEmptyCicdData(data: RepositoryData) {
        data.hasCICD = false
        data.cicdFiles = emptyList()
        data.hasDockerfile = false
    }

    private fun extractFilePaths(response: TreeResponse): List<String> {
        return response.tree
            ?.filter { it.type == "blob" }
            ?.mapNotNull { it.path }
            ?: emptyList()
    }

    private fun findCicdFiles(filePaths: List<String>): List<String> {
        return filePaths.filter { isCicdFile(it) }
    }

    private fun isCicdFile(filePath: String): Boolean {
        if (filePath.matches(GITHUB_WORKFLOWS_PATTERN.toRegex())) return true
        if (JENKINSFILE_PATTERNS.any { filePath.matches(it.toRegex()) }) return true
        return CICD_FILE_PATTERNS.any { filePath.matches(it.toRegex()) }
    }

    private fun findBuildFiles(filePaths: List<String>): List<String> {
        return filePaths.filter { isBuildFile(it) }
    }

    private fun isBuildFile(filePath: String): Boolean {
        return BUILD_FILE_PATTERNS.any { filePath.matches(it.toRegex()) }
    }

    private fun hasDockerFiles(filePaths: List<String>): Boolean {
        return filePaths.any { path ->
            DOCKERFILE_PATTERNS.any { regex -> path.matches(regex.toRegex()) }
        }
    }

    companion object {
        private const val GITHUB_WORKFLOWS_PATTERN = "^\\.github/workflows/.*\\.(yml|yaml)$"

        private val CICD_FILE_PATTERNS = listOf(
            "^\\.gitlab-ci\\.(yml|yaml)$",
            "^\\.travis\\.(yml|yaml)$",
            "^azure-pipelines\\.(yml|yaml)$",
            "^\\.circleci/config\\.(yml|yaml)$",
            "^\\.github/actions/.*\\.(yml|yaml)$",
            "^\\.bitbucket-pipelines\\.(yml|yaml)$",
            "^\\.appveyor\\.(yml|yaml)$"
        )

        private val JENKINSFILE_PATTERNS = listOf(
            ".*/[Jj]enkinsfile$",
            "^[Jj]enkinsfile$",
            ".*/[Jj]enkinsfile\\..+$",
            "^[Jj]enkinsfile\\..+$"
        )

        private val BUILD_FILE_PATTERNS = listOf(
            ".*pom\\.xml$",
            ".*build\\.gradle(\\.kts)?$",
            ".*gradlew(\\.bat)?$",
            ".*mvnw(\\.cmd)?$",
            ".*package\\.json$",
            ".*(yarn\\.lock|pnpm-lock\\.yaml|package-lock\\.json)$",
            ".*requirements\\.txt$",
            ".*setup\\.py$",
            ".*pyproject\\.toml$",
            ".*setup\\.cfg$",
            ".*Cargo\\.toml$",
            ".*go\\.mod$",
            ".*Gemfile(\\.lock)?$",
            ".*composer\\.(json|lock)$",
            ".*CMakeLists\\.txt$",
            ".*Makefile$",
            ".*build\\.xml$"
        )

        private val DOCKERFILE_PATTERNS = listOf(
            ".*(?i)dockerfile.*$",
            ".*(?i)docker-compose.*\\.(yml|yaml)$",
            ".*(?i)compose.*\\.(yml|yaml)$"
        )
    }
}
