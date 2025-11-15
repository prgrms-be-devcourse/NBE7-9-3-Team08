package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.TreeResponse
import org.springframework.stereotype.Component

@Component
class SecurityInfoMapper {

    // ResponseData 보안 정보 매핑
    fun mapSecurityInfo(data: RepositoryData, response: TreeResponse?) {
        val tree = response?.tree.orEmpty()

        if (tree.isEmpty()) {
            setEmptySecurityData(data)
            return
        }

        val allFilePaths = extractFilePaths(tree)

        val sensitiveFiles = findSensitiveFiles(allFilePaths)
        data.hasSensitiveFile = sensitiveFiles.isNotEmpty()
        data.sensitiveFilePaths = sensitiveFiles

        val buildFiles = findBuildFiles(allFilePaths)
        data.hasBuildFile = buildFiles.isNotEmpty()
        data.buildFiles = buildFiles
    }

    private fun setEmptySecurityData(data: RepositoryData) {
        data.hasSensitiveFile = false
        data.sensitiveFilePaths = emptyList()

        data.hasBuildFile = false
        data.buildFiles = emptyList()
    }

    private fun extractFilePaths(tree: List<TreeResponse.TreeItem>): List<String> =
        tree.filter { it.type == "blob" }
            .mapNotNull { it.path }

    private fun findSensitiveFiles(filePaths: List<String>): List<String> =
        filePaths.filter { isSensitiveFile(it) }

    private fun isSensitiveFile(filePath: String): Boolean {
        if (isSafeFile(filePath)) return false

        return SENSITIVE_FILE_PATTERNS.any { pattern ->
            filePath.matches(Regex(pattern))
        }
    }

    private fun isSafeFile(filePath: String): Boolean =
        SAFE_FILE_PATTERNS.any { pattern ->
            filePath.matches(Regex(pattern))
        }

    private fun findBuildFiles(filePaths: List<String>): List<String> =
        filePaths.filter { isBuildFile(it) }

    private fun isBuildFile(filePath: String): Boolean {
        val fileName = extractFileName(filePath)
        return BUILD_FILE_NAMES.contains(fileName)
    }

    private fun extractFileName(filePath: String?): String {
        if (filePath.isNullOrEmpty()) return ""

        val idx = filePath.lastIndexOf('/')
        return if (idx >= 0) filePath.substring(idx + 1) else filePath
    }

    // 민감 파일 패턴
    private companion object {
        val SENSITIVE_FILE_PATTERNS = listOf(
            // 환경 변수 파일
            ".*\\.env$",
            ".*\\.env\\.prod(uction)?$",
            ".*\\.env\\.local$",

            // 인증서 및 키
            ".*\\.(pem|key|p12|pfx|crt|cer|p8)$",
            ".*/id_rsa$|^id_rsa$",
            ".*/id_dsa$|^id_dsa$",
            ".*/id_ecdsa$|^id_ecdsa$",
            ".*/authorized_keys$|^authorized_keys$",
            ".*\\.(keystore|jks)$",

            // 민감한 설정
            ".*application-secret\\.ya?ml$",
            ".*application-prod\\.ya?ml$",
            ".*credentials\\.(json|xml|yml|yaml|properties)$",
            ".*secret.*\\.(json|xml|yml|yaml|properties)$",
            ".*secrets?\\.json$",

            // 클라우드 인증
            ".*/\\.aws/credentials$",
            ".*service-account.*\\.json$",
            ".*firebase.*\\.json$",
            ".*google.*credentials.*\\.json$",

            // 토큰 / API 키
            ".*token.*\\.txt$",
            ".*apikey.*\\.txt$",
            ".*password.*\\.txt$",
            ".*client_secret.*\\.(json|yml|yaml)$",
            ".*oauth.*\\.json$",

            // SSH 관련
            ".*/\\.ssh/id_.*$",
            ".*/\\.ssh/config$",

            // 기타 확실한 민감 파일
            ".*pgpass$",
            ".*\\.netrc$"
        )

        val SAFE_FILE_PATTERNS = listOf(
            // 예시/템플릿 파일들
            ".*\\.(example|template|sample|dist|default)$",
            ".*\\.env\\.(example|template|sample|dist)$",
            ".*credentials\\.(example|sample|template)$",
            ".*secret.*\\.(example|sample|template)$",

            // 테스트/더미 데이터
            ".*test.*\\.(json|yaml|yml|env|properties)$",
            ".*mock.*\\.(json|yaml|yml|env|properties)$",
            ".*dummy.*\\.(json|yaml|yml|env|properties)$",

            // 예시 디렉토리들
            ".*/fixtures/.*",
            ".*/samples?/.*",
            ".*/examples?/.*"
        )

        val BUILD_FILE_NAMES = listOf(
            // 주요 빌드 파일들
            "pom.xml", "build.gradle", "build.gradle.kts",
            "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "Cargo.toml", "go.mod", "requirements.txt", "pyproject.toml", "setup.py",
            "CMakeLists.txt", "Makefile", "Dockerfile",

            // 빌드 스크립트들
            "gradlew", "gradlew.bat", "mvnw", "mvnw.cmd",

            // 언어별 의존성 파일들
            "Gemfile", "Gemfile.lock", "composer.json", "composer.lock",
            "mix.exs", "build.sbt"
        )
    }
}
