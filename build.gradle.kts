plugins {
	java
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "hhplus"
version = "0.0.1-SNAPSHOT"
description = "hhplus study"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
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

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa") //jpa
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson-spring-boot-starter:3.52.0")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j") // mysql connector
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Spring Boot Testcontainers 지원
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Testcontainers 코어
    testImplementation("org.testcontainers:junit-jupiter")
    // MySQL 컨테이너
    testImplementation("org.testcontainers:mysql")
    // Redis Testcontainers
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    // 비동기 작업 완료 대기
    testImplementation("org.awaitility:awaitility:4.2.0")
    // kafka
    testImplementation("org.testcontainers:kafka")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // 테스트 실행 시 자동으로 test 프로필 활성화
    systemProperty("spring.profiles.active", "test")

    // 추가 JVM 옵션
    jvmArgs("-Xmx2048m", "-XX:MaxMetaspaceSize=512m")

    // 테스트 로그 상세 출력
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Properties 파일 UTF-8 인코딩 처리
tasks.withType<ProcessResources> {
    filteringCharset = "UTF-8"
}
