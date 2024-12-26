plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.liquibase.gradle") version "2.2.0"
}

group = "net.dimatomp"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("io.github.jseproject:jse-spi-aac:1.0.2")
	implementation("com.tianscar.javasound:javasound-alac:0.2.3")
	implementation("org.jcodec:jcodec:0.2.5")
	implementation("org.sheinbergon:jna-aac-encoder:2.1.0")
	implementation("org.mp4parser:muxer:1.9.56")
	runtimeOnly("org.liquibase:liquibase-core:4.24.0")
	runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

	liquibaseRuntime("org.liquibase:liquibase-core:4.24.0")
    liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:2.1.1")
    liquibaseRuntime("info.picocli:picocli:4.7.5")
    liquibaseRuntime("org.yaml:snakeyaml:1.33")
	liquibaseRuntime("com.mysql:mysql-connector-j:8.4.0")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

liquibase {
    activities.run {
        register("main") {
			arguments = mapOf(
                "driver" to "com.mysql.cj.jdbc.Driver",
                "changelogFile" to "db/changelog/db.changelog-master.yaml",
				"url" to "jdbc:mysql://localhost:3306/voice_records",
				"username" to "root",
				"password" to "12345678",
				"classpath" to "$rootDir/src/main/resources"
			)
        }
    }
}


kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
