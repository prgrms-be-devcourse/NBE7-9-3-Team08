package com.backend.global.springdoc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "팀 파릇파릇 2차 프로젝트 API 서버",
        version = "beta",
        description = """
            데브코스 백엔드 7회차 9기 8팀 파릇파릇 API 서버 문서입니다.

            API 그룹 안내
            - /api/auth      : 로그인 / 로그아웃 / 인증 관련
            - /api/user      : 회원 정보 관련
            - /api/analysis  : GitHub 저장소 분석 관련
            - /api/community : 커뮤니티 관련 (댓글 등)
            - /api/ai        : OpenAI 기반 AI 분석 관련
        """
    )
)
class OpenApiConfig {

    @Bean
    fun authApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch("/api/auth/**")
            .build()

    @Bean
    fun userApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("user")
            .pathsToMatch("/api/user/**")
            .build()

    @Bean
    fun analysisApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("analysis")
            .pathsToMatch("/api/analysis/**")
            .build()

    @Bean
    fun communityApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("community")
            .pathsToMatch("/api/community/**")
            .build()

    @Bean
    fun aiApi(): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group("ai")
            .pathsToMatch("/api/ai/**")
            .build()
}
