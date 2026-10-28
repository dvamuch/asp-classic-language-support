import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
}

group = "dvamuch"
version = "1.1.0"

// Grammar-Kit loads IntelliJ services in forked Java processes. Keep their
// runtime state outside the immutable IDE SDK extracted into Gradle's cache.
val ideBuildRuntimeDir = layout.buildDirectory.dir("idea-build-runtime").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath").orNull
        if (localIdePath != null) local(localIdePath) else phpstorm("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.27397"
        }

        changeNotes = """
            <ul>
              <li>Adds selection-aware formatting for ASP, INC and standalone VBS files.</li>
              <li>Uses file-wide VBScript control-flow context while keeping lines outside the selection unchanged.</li>
              <li>Preserves safe ASP delimiter boundaries, Undo, mixed line endings and second-pass idempotence.</li>
              <li>Fixes indentation after multiline block-opening statements joined with the VBScript continuation marker.</li>
              <li>Uses HTML comments for template markup and VBScript comments for ASP code when toggling line comments.</li>
            </ul>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.environmentVariable("PUBLISH_CHANNEL")
            .map { listOf(it) }
            .orElse(listOf("default"))
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.PhpStorm, "2025.2.4") {
                useInstaller = false
            }
            // Keep the stable release matrix reproducible. A locally installed
            // EAP can be added explicitly because moving EAP coordinates may
            // disappear or temporarily ship with an incomplete plugin layout.
            create(IntelliJPlatformType.PhpStorm, "2026.2.3") {
                useInstaller = false
            }
            providers.gradleProperty("futurePhpstormIdePath").orNull?.let { localPath ->
                local(file(localPath))
            }
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2026.2.2") {
                useInstaller = false
            }
            create(IntelliJPlatformType.WebStorm, "2026.2.2") {
                useInstaller = false
            }
            providers.gradleProperty("pycharmIdePath").orNull?.let { localPath ->
                local(file(localPath))
            } ?: create(IntelliJPlatformType.PyCharmProfessional, "2026.2.3") {
                useInstaller = false
            }
        }
    }
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    runIde {
        jvmArgs("-Dide.internal=true")
    }
}

tasks.withType<Test>().configureEach {
    // Keep IDE runtime state out of Gradle's immutable transformed SDK. Recent
    // platform builds otherwise place telemetry logs under the SDK itself,
    // which makes the next Gradle invocation reject its own cache entry.
    val ideRuntimeDir = layout.buildDirectory.dir("idea-test-runtime/$name")
    systemProperty("idea.system.path", ideRuntimeDir.map { it.dir("system").asFile.absolutePath }.get())
    systemProperty("idea.config.path", ideRuntimeDir.map { it.dir("config").asFile.absolutePath }.get())
    systemProperty("idea.log.path", ideRuntimeDir.map { it.dir("log").asFile.absolutePath }.get())

    providers.gradleProperty("expectedPlatformBuild").orNull?.let { expectedBuild ->
        systemProperty("asp.expected.platform.build", expectedBuild)
    }

    // Platform 262/JDK 25 exposes Kotlin companion and helper classes to
    // Gradle's JUnit scanner. Only top-level test classes are executable.
    include("**/*Test.class")

    if (providers.gradleProperty("crossIdeSmoke").isPresent) {
        filter {
            includeTestsMatching("*.AspFormatterIntegrationTest")
            includeTestsMatching("*.VbCompletionIntegrationTest")
            includeTestsMatching("*.VbResolveIntegrationTest")
            includeTestsMatching("*.VbIncludeFindUsagesTest")
            includeTestsMatching("*.AspMissingIncludeInspectionTest")
            includeTestsMatching("*.VbUnresolvedIdentifierInspectionTest")
        }
    }

    providers.gradleProperty("ttsProjectDir").orNull?.let { ttsProjectDir ->
        systemProperty("tts.project.dir", ttsProjectDir)
        maxHeapSize = "2g"
        testLogging.events(org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT)

        mapOf(
            "ttsBatchSize" to "tts.batch.size",
            "ttsBatchIndex" to "tts.batch.index",
            "ttsPathFilter" to "tts.path.filter",
            "ttsEditorPathFilter" to "tts.editor.path.filter",
            "ttsHtmlAlignText" to "tts.html.align.text",
            "ttsFormatDiagnosticsDir" to "tts.format.diagnostics.dir",
            "ttsFormatPerformanceTrace" to "asp.format.performance.trace"
        ).forEach { (gradleProperty, systemPropertyName) ->
            providers.gradleProperty(gradleProperty).orNull?.let { value ->
                systemProperty(systemPropertyName, value)
            }
        }
    }
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    from("LICENSE") {
        into("META-INF")
    }
}

tasks.named<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>("prepareTestSandbox") {
    // Vue LSP assumes its production plugin classloader layout and fails in the
    // platform test classloader. ASP tests still exercise bundled HTML/JS/CSS.
    disabledPlugins.add("org.jetbrains.plugins.vue")

    // IntelliJ IDEA Ultimate contains an obfuscated post-startup extension
    // whose constructor cannot be instantiated by the headless test container.
    // It is unrelated to ASP support, so omit only that IDEA product layer for
    // cross-IDE smoke tests. Other IDEs need their product module intact.
    providers.gradleProperty("localIdePath").orNull
        ?.takeIf {
            it.contains("idea-", ignoreCase = true) ||
                it.contains("IntelliJ IDEA", ignoreCase = true)
        }
        ?.let { disabledPlugins.add("com.intellij.modules.ultimate") }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    // Newer IDE SDKs advertise their own runtime level (Java 25 in 2026.2).
    // Keep the produced plugin bytecode at the oldest supported runtime so the
    // same archive remains loadable by PhpStorm 2025.2.x (Java 21).
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

sourceSets {
    main {
        java.srcDir("src/main/gen")
    }
}

tasks {
    val generateVbScriptParser by registering(org.jetbrains.grammarkit.tasks.GenerateParserTask::class) {
        sourceFile.set(file("src/main/grammar/VbScript.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToParser.set("dvamuch/aspclassiclanguagesupport2/lang/vbscript/parser/VbScriptParser.java")
        pathToPsiRoot.set("dvamuch/aspclassiclanguagesupport2/lang/vbscript/psi")
        purgeOldFiles.set(true)
    }

    val generateVbScriptLexer by registering(org.jetbrains.grammarkit.tasks.GenerateLexerTask::class) {
        sourceFile.set(file("src/main/grammar/VbScript.flex"))
        targetOutputDir.set(file("src/main/gen/dvamuch/aspclassiclanguagesupport2/lang/vbscript"))
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(generateVbScriptParser, generateVbScriptLexer)
    }
    withType<JavaCompile> {
        dependsOn(generateVbScriptParser, generateVbScriptLexer)
    }
    withType<org.jetbrains.grammarkit.tasks.GenerateParserTask> {
        systemProperty("idea.system.path", ideBuildRuntimeDir.dir("parser/system").asFile.absolutePath)
        systemProperty("idea.config.path", ideBuildRuntimeDir.dir("parser/config").asFile.absolutePath)
        systemProperty("idea.log.path", ideBuildRuntimeDir.dir("parser/log").asFile.absolutePath)
        classpath += configurations.detachedConfiguration(
            project.dependencies.create("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8"),
            project.dependencies.create("io.opentelemetry:opentelemetry-api:1.28.0")
        )
    }
    withType<org.jetbrains.grammarkit.tasks.GenerateLexerTask> {
        systemProperty("idea.system.path", ideBuildRuntimeDir.dir("lexer/system").asFile.absolutePath)
        systemProperty("idea.config.path", ideBuildRuntimeDir.dir("lexer/config").asFile.absolutePath)
        systemProperty("idea.log.path", ideBuildRuntimeDir.dir("lexer/log").asFile.absolutePath)
    }
}
