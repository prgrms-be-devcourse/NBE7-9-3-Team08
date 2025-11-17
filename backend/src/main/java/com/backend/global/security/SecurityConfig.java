package com.backend.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
//@RequiredArgsConstructor
public class SecurityConfig {
/*    private final JwtUtil jwtUtil;
    private final JwtService jwtService;*/

    //private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // JWT 인증을 사용하므로 세션을 사용하지 않음 (Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // H2 콘솔은 frameOptions 해제 필요 (iframe으로 동작)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )

                // CSRF 공격 방지 비활성화 (Stateless API에서 주로 사용)
                // H2 콘솔은 CSRF 예외로 설정
                .csrf(csrf -> csrf.disable())

                // cors 설정 추가
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
                    corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    corsConfiguration.setExposedHeaders(List.of("Authorization"));
                    return corsConfiguration;
                }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/login", //로그인
                                "/api/auth",  //이메인 인증코드 전송
                                "/api/verify",//이메일 인증코드 검증
                                "/api/user",  //회원가입
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/h2-console/**" // H2 콘솔 허용
                        ).permitAll()
                        .requestMatchers(
                                // GET만 허용 - 조회는 인증 불필요
                                HttpMethod.GET,
                                "/api/analysis/stream/**",
                                "/api/analysis/repositories/{repositoriesId}",
                                "/api/analysis/repositories/{repositoryId}/results/{analysisId}",
                                "/api/community/repositories",
                                "/api/community/*/comments"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())
                //커스텀 JWT 필터를 등록
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BcryptEncoder를 Bean으로 등록합니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
