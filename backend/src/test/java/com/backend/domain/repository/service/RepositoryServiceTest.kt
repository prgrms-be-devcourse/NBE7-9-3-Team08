package com.backend.domain.repository.service

import com.backend.domain.repository.repository.RepositoryJpaRepository
import com.backend.domain.user.entity.User
import com.backend.domain.user.repository.UserRepository
import com.backend.global.exception.BusinessException
import com.backend.global.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@Tag("integration")
internal class RepositoryServiceTest {

    companion object {
        private val log = LoggerFactory.getLogger(RepositoryServiceTest::class.java)
    }

    @Autowired
    private lateinit var repositoryService: RepositoryService

    @Autowired
    private lateinit var repositoryJpaRepository: RepositoryJpaRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        val user = User(
            "test${System.currentTimeMillis()}@test.com",
            "password123",
            "Test User"
        )
        testUser = userRepository.save(user)
    }

    @Test
    @DisplayName("README, TEST, CI/CD가 없어도 RepositoryData 수집은 정상적으로 완료된다")
    fun testRepositoryWithoutSomeFeatures1() {
        val owner = "prgrms-be-devcourse"
        val repo = "NBE7-9-2-Team01"
        val userId = testUser.id!!

        val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)

        assertThat(data).isNotNull
        log.info("📦 수집된 RepositoryData {}", data)

        val repoEntity = repositoryJpaRepository.findByHtmlUrlAndUserId(data.repositoryUrl, userId)
        assertThat(repoEntity).isNotNull
    }

    @Test
    @DisplayName("README, TEST, CI/CD가 없어도 RepositoryData 수집은 정상적으로 완료된다")
    fun testRepositoryWithoutSomeFeatures2() {
        val owner = "prgrms-be-devcourse"
        val repo = "NBE7-9-2-Team02"
        val userId = testUser.id!!

        val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)

        assertThat(data).isNotNull
        log.info("📦 수집된 RepositoryData {}", data)

        val repoEntity = repositoryJpaRepository.findByHtmlUrlAndUserId(data.repositoryUrl, userId)
        assertThat(repoEntity).isNotNull
    }

    @Test
    @DisplayName("README, TEST, CI/CD가 없어도 RepositoryData 수집은 정상적으로 완료된다")
    fun testRepositoryWithoutSomeFeatures3() {
        val owner = "prgrms-be-devcourse"
        val repo = "NBE7-9-2-Team3"
        val userId = testUser.id!!

        val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)

        assertThat(data).isNotNull
        log.info("📦 수집된 RepositoryData {}", data)

        val repoEntity = repositoryJpaRepository.findByHtmlUrlAndUserId(data.repositoryUrl, userId)
        assertThat(repoEntity).isNotNull
    }

    @Test
    @DisplayName("README, TEST, CI/CD가 없어도 RepositoryData 수집은 정상적으로 완료된다")
    fun testRepositoryWithoutSomeFeatures4() {
        val owner = "prgrms-be-devcourse"
        val repo = "NBE7-9-2-Team04"
        val userId = testUser.id!!

        val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)

        assertThat(data).isNotNull
        log.info("📦 수집된 RepositoryData {}", data)

        val repoEntity = repositoryJpaRepository.findByHtmlUrlAndUserId(data.repositoryUrl, userId)
        assertThat(repoEntity).isNotNull
    }

    @Test
    @DisplayName("완벽한 README + 테스트 + CI/CD를 갖춘 저장소")
    fun testWellStructuredRepository() {
        val owner = "spring-projects"
        val repo = "spring-boot"
        val userId = testUser.id!!

        val data = repositoryService.fetchAndSaveRepository(owner, repo, userId)
        log.info("📦 수집된 RepositoryData {}", data)

        assertThat(data).isNotNull
        assertThat(data.hasReadme).isTrue()
        assertThat(data.readmeLength).isGreaterThan(1000)
        assertThat(data.hasTestDirectory).isTrue()
        assertThat(data.hasCICD).isTrue()
        assertThat(data.testCoverageRatio).isGreaterThan(0.0)
    }

    @Test
    @DisplayName("용량 큰 저장소의 경우 분석 불가")
    fun testActiveRepository() {
        val owner = "facebook"
        val repo = "react"
        val userId = testUser.id!!

        assertThatThrownBy {
            repositoryService.fetchAndSaveRepository(owner, repo, userId)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.GITHUB_REPO_TOO_LARGE)

        log.info("✅ 용량 큰 저장소는 분석 불가능(GITHUB_REPO_TOO_LARGE)")
    }

    @Test
    @DisplayName("존재하지 않는 저장소 요청 시 BusinessException(GITHUB_REPO_NOT_FOUND) 발생")
    fun testRepositoryNotFound() {
        val owner = "prgrms-be-devcourse"
        val repo = "NBE7-9-2-Team0"
        val userId = testUser.id!!

        assertThatThrownBy {
            repositoryService.fetchAndSaveRepository(owner, repo, userId)
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.GITHUB_REPO_NOT_FOUND)

        log.info("✅ 존재하지 않는 저장소는 정상적으로 예외 발생(GITHUB_REPO_NOT_FOUND)")
    }
}
