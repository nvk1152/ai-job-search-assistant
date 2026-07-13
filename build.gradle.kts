plugins {
    id("java")
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.nvk.jsa"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // The JVM's default timezone can resolve to a legacy IANA alias (e.g. "Asia/Calcutta" on
        // Windows) that Postgres's tzdata rejects during the JDBC startup handshake.
        jvmArgs("-Duser.timezone=UTC")
    }
}
