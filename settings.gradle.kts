plugins {
    // Gradle 8+ no longer ships a default JDK provisioner. Download the Java
    // toolchain required by the target IntelliJ Platform when it is not
    // installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "asp-classic-language-support"
