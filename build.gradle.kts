plugins {
    kotlin("jvm") version "1.9.23"
}

val lwjglVersion = "3.3.4"
val lwjglNatives = "natives-windows"

group = "me.mason"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.joml:joml:1.10.8")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.compileKotlin.get().kotlinOptions {
    languageVersion = "1.9"
    jvmTarget = "17"
    freeCompilerArgs = listOf(
        "-Xcontext-receivers", "-Xinline-classes",
        "-Xopt-in=kotlin.time.ExperimentalTime",
        "-Xopt-in=kotlin.contracts.ExperimentalContracts",
        "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
        "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
        "-Xopt-in=kotlinx.coroutines.InternalCoroutinesApi"
    )
}