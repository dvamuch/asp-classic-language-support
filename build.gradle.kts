plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.grammarkit") version "2022.3.2"
}

group = "dvamuch"
version = "v0.0.3"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        phpstorm("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)


        // Add plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            Stable mixed ASP/HTML formatting with VBScript control-flow indentation.
        """.trimIndent()
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
        doFirst {
            val sandboxDir = file("build/idea-sandbox")
            if (sandboxDir.exists()) {
                sandboxDir.walkTopDown()
                    .filter { it.isFile && it.name == "idea.log" }
                    .forEach { it.writeText("") }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    providers.gradleProperty("ttsProjectDir").orNull?.let { ttsProjectDir ->
        systemProperty("tts.project.dir", ttsProjectDir)
        maxHeapSize = "2g"
        testLogging.events(org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT)

        mapOf(
            "ttsBatchSize" to "tts.batch.size",
            "ttsBatchIndex" to "tts.batch.index",
            "ttsPathFilter" to "tts.path.filter"
        ).forEach { (gradleProperty, systemPropertyName) ->
            providers.gradleProperty(gradleProperty).orNull?.let { value ->
                systemProperty(systemPropertyName, value)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
        targetRoot.set("src/main/gen")
        pathToParser.set("dvamuch/aspclassiclanguagesupport2/lang/vbscript/parser/VbScriptParser.java")
        pathToPsiRoot.set("dvamuch/aspclassiclanguagesupport2/lang/vbscript/psi")
        purgeOldFiles.set(true)
    }

    val generateVbScriptLexer by registering(org.jetbrains.grammarkit.tasks.GenerateLexerTask::class) {
        sourceFile.set(file("src/main/grammar/VbScript.flex"))
        targetDir.set("src/main/gen/dvamuch/aspclassiclanguagesupport2/lang/vbscript")
        targetClass.set("VbScriptLexer")
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
