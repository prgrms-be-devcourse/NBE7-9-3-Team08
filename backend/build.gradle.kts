plugins {
    java
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }

    sourceSets {
        val test by getting {
            kotlin.srcDirs("src/test/java", "src/test/kotlin")
        }
    }
}


dependencies {

    // -----------------------------------------------------------------------------------------
    // Spring Boot Core & Web
    // -----------------------------------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient 용

    // -----------------------------------------------------------------------------------------
    // Database / Persistence
    // -----------------------------------------------------------------------------------------
    runtimeOnly("com.h2database:h2")                  // H2 (local test)
    runtimeOnly("com.mysql:mysql-connector-j")        // MySQL driver

    // -----------------------------------------------------------------------------------------
    // Infra: Mail / Redis / Retry
    // -----------------------------------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.retry:spring-retry")

    // -----------------------------------------------------------------------------------------
    // JWT (io.jsonwebtoken)
    // -----------------------------------------------------------------------------------------
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // -----------------------------------------------------------------------------------------
    // OpenAPI / Swagger
    // -----------------------------------------------------------------------------------------
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // -----------------------------------------------------------------------------------------
    // OpenAI API + 환경변수(.env) Loader
    // -----------------------------------------------------------------------------------------
    implementation("com.openai:openai-java:4.3.0")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    // -----------------------------------------------------------------------------------------
    // Kotlin Standard Library
    // -----------------------------------------------------------------------------------------
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")


    // -----------------------------------------------------------------------------------------
    // 개발 편의 — DevTools
    // -----------------------------------------------------------------------------------------
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // -----------------------------------------------------------------------------------------
    // Lombok (컴파일 전용)
    // -----------------------------------------------------------------------------------------
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    // -----------------------------------------------------------------------------------------
    // Test
    // -----------------------------------------------------------------------------------------
    // Spring Boot 기본 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")

    // Assertion
    testImplementation("org.assertj:assertj-core:3.26.3")

    // Kotlin Test 환경 (MockK + SpringMockK)
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-jvm:1.13.12")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")

    // JUnit 런처
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.withType<Test> {
    useJUnitPlatform {
        // CI 환경에서 테스트 제외
        if (System.getenv("CI") == "true") {
            excludeTags("redis", "integration")
        }
    }
}