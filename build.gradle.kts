plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.jooq.jooq-codegen-gradle") version "3.19.35"
    id("org.jmailen.kotlinter") version "5.7.0"
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
    // DDLDatabase: jOOQ generates the classes by parsing the Flyway migration
    // scripts directly, so db/migration is the single source of truth.
    jooqCodegen("org.jooq:jooq-meta-extensions:3.19.35")

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
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
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
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                includes = ".*"
                excludes = "flyway_schema_history"
                // the DDL interpreter puts unqualified objects into H2's PUBLIC schema;
                // map it back to postgres' lower-case public
                schemata {
                    schema {
                        inputSchema = "PUBLIC"
                        outputSchema = "public"
                    }
                }
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                    property {
                        key = "unqualifiedSchema"
                        value = "public"
                    }
                }
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
    }
}

// the codegen plugin does not know that the Flyway scripts are the codegen input,
// so declare them explicitly to keep up-to-date checks honest
tasks.named("jooqCodegen") {
    inputs
        .files(fileTree("src/main/resources/db/migration"))
        .withPropertyName("flywayMigrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// compile the jOOQ-generated sources; adding them to the task (not the source set)
// keeps them out of kotlinter, which lints the Kotlin source set
tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
    dependsOn("jooqCodegen")
    source(layout.buildDirectory.dir("generated/jooq"))
}

// The jOOQ codegen plugin auto-wires target.directory into the Java source set,
// which the Kotlin plugin merges into the Kotlin source set that kotlinter lints.
// Drop the generated dir from the source sets so kotlinter only sees hand-written code.
val jooqGenDir = layout.buildDirectory.dir("generated/jooq").get().asFile
afterEvaluate {
    val sourceSets = extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    for (name in listOf("main", "test")) {
        val ss = sourceSets.getByName(name)
        val javaSrc: org.gradle.api.file.SourceDirectorySet = ss.java
        javaSrc.setSrcDirs(javaSrc.srcDirs.filter { it.absolutePath != jooqGenDir.absolutePath })
    }
    val kotlinExt = extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java)
    val kotlinMain: org.gradle.api.file.SourceDirectorySet =
        kotlinExt.sourceSets.getByName("main").kotlin
    kotlinMain.setSrcDirs(kotlinMain.srcDirs.filter { it.absolutePath != jooqGenDir.absolutePath })
}

tasks.withType<Test> {
    useJUnitPlatform()
    // The tests share a single embedded PostgreSQL instance (see TestDatabase/TestPostgres),
    // so they must not run in parallel forks.
    maxParallelForks = 1
    systemProperty("GRADLE_PARALLEL_WORKERS", 1)
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
