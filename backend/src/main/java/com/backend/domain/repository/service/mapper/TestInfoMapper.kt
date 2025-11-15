package com.backend.domain.repository.service.mapper

import com.backend.domain.repository.dto.response.RepositoryData
import com.backend.domain.repository.dto.response.github.TreeResponse
import org.springframework.stereotype.Component

@Component
class TestInfoMapper {
    fun mapTestInfo(data: RepositoryData, response: TreeResponse?) {
        val tree = response?.tree.orEmpty()

        if (tree.isEmpty()) {
            setEmptyTestData(data)
            return
        }

        val allPaths = extractAllPaths(tree)
        val filePaths = extractFilePaths(tree)

        // 1. 테스트 디렉터리 존재 여부
        data.hasTestDirectory = checkTestDirectoryExists(allPaths)

        // 2. 테스트 파일 개수
        val testFileCount = countTestFiles(filePaths)
        data.testFileCount = testFileCount

        // 3. 소스 파일 개수
        val sourceFileCount = countSourceFiles(filePaths)
        data.sourceFileCount = sourceFileCount

        // 4. 테스트 커버리지 비율
        data.testCoverageRatio = calculateTestCoverageRatio(testFileCount, sourceFileCount)
    }

    private fun setEmptyTestData(data: RepositoryData) {
        data.hasTestDirectory = false
        data.testFileCount = 0
        data.sourceFileCount = 0
        data.testCoverageRatio = 0.0
    }

    private fun extractAllPaths(tree: List<TreeResponse.TreeItem>): List<String> =
        tree.mapNotNull { it.path }

    private fun extractFilePaths(tree: List<TreeResponse.TreeItem>): List<String> =
        tree.filter { it.type == "blob" }
            .mapNotNull { it.path }

    private fun checkTestDirectoryExists(allPaths: List<String>): Boolean =
        TEST_DIRECTORY_PATTERNS.any { pattern ->
            val regex = Regex(pattern)
            allPaths.any { it.matches(regex) }
        }

    private fun countTestFiles(filePaths: List<String>): Int =
        filePaths.count { isTestFile(it) }

    private fun isTestFile(filePath: String): Boolean {
        val inTestDirectory = TEST_DIRECTORY_PATTERNS.any { pattern ->
            filePath.matches(Regex(pattern))
        }

        val matchesTestPattern = TEST_FILE_PATTERNS.any { pattern ->
            filePath.matches(Regex(pattern))
        }

        return inTestDirectory || matchesTestPattern
    }

    private fun countSourceFiles(filePaths: List<String>): Int =
        filePaths.count { isSourceFile(it) && !isTestFile(it) }

    private fun isSourceFile(filePath: String): Boolean =
        SOURCE_FILE_EXTENSIONS.any { ext -> filePath.endsWith(ext) }

    private fun extractFileName(filePath: String?): String {
        if (filePath.isNullOrEmpty()) return ""
        val idx = filePath.lastIndexOf('/')
        return if (idx >= 0) filePath.substring(idx + 1) else filePath
    }

    private fun calculateTestCoverageRatio(testFileCount: Int, sourceFileCount: Int): Double {
        if (sourceFileCount == 0) return 0.0
        val ratio = testFileCount.toDouble() / sourceFileCount
        return kotlin.math.round(ratio * 1000.0) / 1000.0
    }

    companion object {
        // 테스트 디렉터리
        private val TEST_DIRECTORY_PATTERNS = listOf(
            "^src/test/.*",
            ".*/(test|tests|spec|specs|__tests__)/.*",
            ".*/(integration-tests?|functional-tests?|acceptance-tests?|e2e|qa|itest|utest)/.*"
        )

        // 테스트 파일
        private val TEST_FILE_PATTERNS = listOf(
            // Java/Kotlin
            ".*(Test|Tests|TestCase|IT|Spec|Feature|Scenario)\\.(java|kt)$",
            ".*(Integration|Application|Unit|Functional|E2E|Performance|Load|Smoke|Acceptance|Regression|UITest).*Test\\.(java|kt)$",
            ".*(TestBase|TestUtils?|TestHelper|TestData|TestConfig|TestSuite)\\.(java|kt)$",

            // JavaScript/TypeScript
            ".*\\.(test|spec|e2e-spec|integration|unit|browser)\\.(js|ts|jsx|tsx)$",

            // Python
            "^test_.*\\.py$",
            ".*_test\\.py$",

            // Go
            ".*_test\\.go$",
            ".*_(integration|unit)_test\\.go$",

            // Ruby
            ".*_spec\\.rb$",

            // Rust
            ".*_test\\.rs$",
            ".*/tests\\.rs$",

            // C/C++
            ".*[Tt]est.*\\.(c|cpp|cc|cxx|h|hpp)$",

            // Dart
            ".*_test\\.dart$",

            // PHP / Swift / C#
            ".*Test\\.(php|swift|cs)$",

            // 기타 패턴
            ".*(Mock|Validator|Controller|Service|Repository|API|UI)Test\\.(java|kt|ts|py|cs|php|swift)$"
        )

        // 소스 파일 확장자
        private val SOURCE_FILE_EXTENSIONS = listOf(
            ".java", ".kt", ".scala", ".js", ".ts", ".jsx", ".tsx",
            ".py", ".rb", ".go", ".rs", ".cpp", ".c", ".cs",
            ".php", ".swift", ".m", ".mm", ".dart", ".h", ".hpp"
        )
    }
}
