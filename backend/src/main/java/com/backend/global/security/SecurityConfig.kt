package com.backend.global.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        http // JWT 인증을 사용하므로 세션을 사용하지 않음 (Stateless)
            .sessionManagement(Customizer { session->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            })

            // H2 콘솔은 frameOptions 해제 필요 (iframe으로 동작)
            .headers(Customizer { headers ->
                headers.frameOptions(Customizer { frame-> frame.disable() })
            }
            )
            // CSRF 공격 방지 비활성화 (Stateless API에서 주로 사용)
            .csrf(Customizer { csrf-> csrf.disable() })

            .authorizeHttpRequests(Customizer { auth ->
                auth
                    .requestMatchers(
                        "/api/login",  //로그인
                        "/api/auth",  //이메인 인증코드 전송
                        "/api/verify",  //이메일 인증코드 검증
                        "/api/user",  //회원가입
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/h2-console/**" // H2 콘솔 허용
                    ).permitAll()
                    .requestMatchers(
                        "/api/analysis/stream/**",
                        "/api/analysis/repositories/{repositoriesId}",
                        "/api/analysis/repositories/{repositoryId}/results/{analysisId}",
                        "/api/repositories/**",
                        "/api/ai/complete/**",
                        "/api/community/repositories",
                        "/api/community/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            )
            .formLogin(Customizer { login -> login.disable() })
            .httpBasic(Customizer { basic -> basic.disable() })

            //커스텀 JWT 필터를 등록
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    // BcryptEncoder를 Bean으로 등록합니다.
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
