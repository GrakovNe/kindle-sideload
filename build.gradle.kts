plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.jooq.jooq-codegen-gradle") version "3.19.35"
    jacoco
}

group = "org.grakovne"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    jooqCodegen("com.h2database:h2")

    implementation("com.github.pengrad:java-telegram-bot-api:10.1.0")

    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("org.apache.commons:commons-text:1.15.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.jooq:jooq")
    implementation("org.jooq:jooq-kotlin")

    implementation("net.lingala.zip4j:zip4j:2.11.6")
    implementation("org.apache.commons:commons-lang3:3.20.0")

    implementation("com.ibm.icu:icu4j:78.3")

    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    implementation("org.reflections:reflections:0.10.2")

    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation(kotlin("test"))
}

jacoco {
    toolVersion = "0.8.15"
}

jooq {
    configuration {
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            strategy {
                name = "org.jooq.codegen.DefaultGeneratorStrategy"
            }
            database {
                name = "org.jooq.meta.h2.H2Database"
                inputSchema = "public"
                includes = ".*"
                excludes = "flyway_schema_history"
            }
            generate {
                javaTimeTypes = true
                pojos = true
                daos = false
                records = true
            }
            target {
                packageName = "org.grakovne.sideload.kindle.generated"
                directory = "build/generated/jooq"
            }
        }
        jdbc {
            driver = "org.h2.Driver"
            url = "jdbc:h2:mem:jooq_gen;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'src/main/resources/db/schema.sql'"
            user = "sa"
            password = ""
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("jooqCodegen")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    source("build/generated/jooq")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
