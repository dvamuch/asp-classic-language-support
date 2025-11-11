plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
    id("org.jetbrains.grammarkit") version "2022.3.2"
}

group = "dvamuch"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1.4.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Настраиваем генерацию JFlex вручную
tasks.register<Exec>("generateAspLexer") {
    workingDir = projectDir
    commandLine = listOf(
        "java", "-jar",
        "/path/to/jflex.jar", // Укажите путь к jflex.jar
        "-d", "src/main/kotlin/com/dvamuch/asp",
        "src/main/kotlin/com/dvamuch/asp/asp.flex"
    )
}

tasks.named("compileKotlin") {
    dependsOn("generateAspLexer")
}
