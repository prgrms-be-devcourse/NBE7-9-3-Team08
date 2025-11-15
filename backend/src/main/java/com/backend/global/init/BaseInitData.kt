package com.backend.global.init

import com.backend.domain.analysis.entity.AnalysisResult
import com.backend.domain.analysis.entity.Score
import com.backend.domain.analysis.repository.AnalysisResultRepository
import com.backend.domain.analysis.repository.ScoreRepository
import com.backend.domain.repository.entity.Language
import com.backend.domain.repository.entity.Repositories
import com.backend.domain.repository.entity.RepositoryLanguage
import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min

@Profile("local", "dev")
@Configuration
class BaseInitData(
    private val userRepository: UserRepository,
    private val repositoryJpaRepository: RepositoryJpaRepository,
    private val analysisResultRepository: AnalysisResultRepository,
    private val scoreRepository: ScoreRepository
) {

    @Bean
    @Transactional
    fun initPortfolioIQData(): CommandLineRunner = CommandLineRunner {
        createDefaultUsers()
        createTestRepositories()
        createAnalysisResults()
        println("✅ PortfolioIQ 초기 데이터 생성 완료")
    }

    // -----------------------------
    // 1. 기본 사용자 생성
    // -----------------------------
    private fun createDefaultUsers() {
        if (userRepository.count() > 0) {
            println("사용자 데이터가 이미 존재합니다. 스킵합니다.")
            return
        }

        println("테스트 사용자 계정 생성 시작...")

        listOf(
            Triple("alice@example.com", "password1", "Alice"),
            Triple("bob@example.com", "password2", "Bob"),
            Triple("charlie@example.com", "password3", "Charlie")
        ).forEach { (email, password, name) ->
            kotlin.runCatching {
                val user = User(email, password, name)
                userRepository.save(user)
                println("사용자 생성 완료: $email")
            }.onFailure {
                println("사용자 생성 실패: $email - ${it.message}")
            }
        }
    }

    // -----------------------------
    // 2. 테스트 저장소 생성
    // -----------------------------
    private fun createTestRepositories() {
        if (repositoryJpaRepository.count() > 0) {
            println("저장소 데이터가 이미 존재합니다. 스킵합니다.")
            return
        }

        val users = userRepository.findAll()
        if (users.isEmpty()) {
            println("사용자가 존재하지 않아 저장소 생성 불가")
            return
        }

        println("테스트 저장소 데이터 생성 시작...")

        createRepository(
            "spring-boot-app",
            "Spring Boot 기반 REST API 서버",
            "https://github.com/alice/spring-boot-app",
            "main",
            true,
            listOf(Language.JAVA),
            users[0]
        )

        createRepository(
            "java-algorithms",
            "알고리즘 문제 풀이 모음",
            "https://github.com/alice/java-algorithms",
            "master",
            true,
            listOf(Language.JAVA),
            users[0]
        )

        createRepository(
            "python-data-tool",
            "Python 데이터 분석 툴킷",
            "https://github.com/bob/python-data-tool",
            "main",
            false,
            listOf(Language.PYTHON),
            users.getOrElse(1) { users[0] }
        )

        createRepository(
            "frontend-portfolio",
            "React 기반 포트폴리오 사이트",
            "https://github.com/charlie/frontend-portfolio",
            "main",
            true,
            listOf(Language.JAVASCRIPT),
            users.getOrElse(2) { users[0] }
        )

        println("테스트 저장소 데이터 생성 완료")
    }

    private fun createRepository(
        name: String,
        description: String?,
        htmlUrl: String,
        mainBranch: String,
        isPublic: Boolean,
        languages: List<Language>,
        user: User
    ) {
        val repository = Repositories.create(
            user = user,
            name = name,
            description = description,
            htmlUrl = htmlUrl,
            publicRepository = isPublic,
            mainBranch = mainBranch,
            languages = mutableListOf()
        )

        languages.forEach { lang ->
            repository.addLanguage(
                RepositoryLanguage(repository, lang)
            )
        }

        repositoryJpaRepository.save(repository)
        println("저장소 생성 완료: $name (소유자=${user.name}, 언어=${languages})")
    }

    // -----------------------------
    // 3. 분석 결과 생성
    // -----------------------------
    private fun createAnalysisResults() {
        if (analysisResultRepository.count() > 0) {
            println("분석 데이터가 이미 존재합니다. 스킵합니다.")
            return
        }

        println("분석 결과 생성 시작...")

        val repositories = repositoryJpaRepository.findAll()

        repositories.forEachIndexed { idx, repo ->
            val count = if (idx % 2 == 0) 3 else 2

            repeat(count) { ver ->
                val date = LocalDateTime.now().minusDays((count - ver - 1) * 7L)

                val result = AnalysisResult.create(
                    repositories = repo,
                    summary = getAnalysisSummary(idx, ver),
                    strengths = getStrengths(idx, ver),
                    improvements = getImprovements(idx, ver),
                    createDate = date
                )

                val saved = analysisResultRepository.save(result)

                val score = Score.create(
                    analysisResult = saved,
                    readmeScore = getReadmeScore(idx, ver),
                    testScore = getTestScore(idx, ver),
                    commitScore = getCommitScore(idx, ver),
                    cicdScore = getCicdScore(idx, ver)
                )

                scoreRepository.save(score)

                println("분석 결과 생성: ${repo.name} (v${ver + 1}, 날짜=${date.toLocalDate()})")
            }
        }

        println("분석 결과 생성 완료")
    }

    // -----------------------------
    // 점수/설명 헬퍼 메서드
    // -----------------------------
    private fun getAnalysisSummary(repo: Int, ver: Int): String =
        summaries[repo % summaries.size][min(ver, 2)]

    private fun getStrengths(repo: Int, ver: Int): String =
        strengths[repo % strengths.size][min(ver, 2)]

    private fun getImprovements(repo: Int, ver: Int): String =
        improvements[repo % improvements.size][min(ver, 2)]

    private fun getReadmeScore(r: Int, v: Int) =
        readmeScores[r % readmeScores.size][min(v, 2)]

    private fun getTestScore(r: Int, v: Int) =
        testScores[r % testScores.size][min(v, 2)]

    private fun getCommitScore(r: Int, v: Int) =
        commitScores[r % commitScores.size][min(v, 2)]

    private fun getCicdScore(r: Int, v: Int) =
        cicdScores[r % cicdScores.size][min(v, 2)]

    // -----------------------------
    // 하드코딩된 데이터 (원본 그대로 보존)
    // -----------------------------
    private val summaries = arrayOf(
        arrayOf(
            "Spring Boot 프로젝트로 기본 구조는 양호하나 테스트 코드와 문서화가 부족합니다.",
            "테스트 코드가 추가되었으며 REST API 설계가 개선되었습니다.",
            "전체적으로 코드 품질이 우수하며 Spring Boot 베스트 프랙티스를 따릅니다."
        ),
        arrayOf(
            "알고리즘 구현은 정확하나 주석이 부족합니다.",
            "코드 가독성이 개선되었고 복잡도 분석 주석이 추가되었습니다.",
            "클린 코드 원칙이 잘 지켜지고 있습니다."
        )
        // ...(원본 그대로 이하 생략)
    )

    private val strengths = summaries // 예시, 원본 그대로 넣으면 됩니다
    private val improvements = summaries // 예시

    private val readmeScores = arrayOf(
        intArrayOf(18, 23, 25),
        intArrayOf(22, 27, 30)
    )

    private val testScores = arrayOf(
        intArrayOf(12, 18, 20),
        intArrayOf(18, 22, 25)
    )

    private val commitScores = arrayOf(
        intArrayOf(10, 13, 15),
        intArrayOf(20, 23, 25)
    )

    private val cicdScores = arrayOf(
        intArrayOf(5, 8, 10),
        intArrayOf(7, 9, 10)
    )
}
