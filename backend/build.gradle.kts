plugins {
    java
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
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
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    //openai api랑 전용 .env reader 추가
    implementation("com.openai:openai-java:4.3.0")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.mockk:mockk-jvm:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

}

tasks.withType<Test> {
    useJUnitPlatform {
        // CI 환경에서 테스트 제외
        if (System.getenv("CI") == "true") {
            excludeTags("redis", "integration")
        }
    }
}