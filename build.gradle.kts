plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
}

group = "dvamuch"
version = "1.0.0-rc3"

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
        if (localIdePath != null) local(localIdePath) else phpstorm("2026.2.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("intellij.structureView.plugin")
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262.10315"
        }

        changeNotes = """
            <ul>
              <li>Fixes Find Usages for user-class members declared in included ASP/INC files.</li>
              <li>Keeps same-named members from unrelated classes out of include-aware usage results.</li>
              <li>Replaces deprecated ReadAction API calls for forward compatibility with future IDE releases.</li>
            </ul>
        """.trimIndent()
    }
}


tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "25"
        targetCompatibility = "25"
    }

    runIde {
        jvmArgs("-Dide.internal=true")
    }
}

tasks.withType<Test>().configureEach {
    // Platform 262/JDK 25 exposes Kotlin companion and helper classes to
    // Gradle's JUnit scanner. Only top-level test classes are executable.
    include("**/*Test.class")

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
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
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
        classpath += configurations.detachedConfiguration(
            project.dependencies.create("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8"),
            project.dependencies.create("io.opentelemetry:opentelemetry-api:1.28.0")
        )
    }
}
