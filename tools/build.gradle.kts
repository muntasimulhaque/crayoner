// The offline asset generators are plain JVM Kotlin: the picture sheets used
// for review, the launcher icons, the store art, and the sound effects. They
// share :core with the app, so a generator can never draw a picture the game
// does not play.
plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation("junit:junit:4.13.2")
}

tasks.register<JavaExec>("makeSheets") {
    group = "tools"
    description = "Render every page as a sample and as line art into build/sheets, for review."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.MakeSheetsKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("makeSounds") {
    group = "tools"
    description = "Regenerate the four sound effects in app/src/main/res/raw."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.SoundGenKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkSounds") {
    group = "tools"
    description = "Verify the committed sound assets match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.SoundGenKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeIcons") {
    group = "tools"
    description = "Regenerate the launcher icon set in app/src/main/res."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath)
}

tasks.register<JavaExec>("checkIcons") {
    group = "tools"
    description = "Verify the committed launcher icons match a fresh regeneration."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.MakeIconsKt"
    args = listOf(rootDir.absolutePath, "--check")
}

tasks.register<JavaExec>("makeArt") {
    group = "tools"
    description = "Regenerate the store art (feature graphic, store icon)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.MakeArtKt"
    args = listOf(rootDir.absolutePath)
}

// A review tool, not a generator: it writes nothing the app ships. Point it
// at any PNG (a screenshot, a sheet) to crop and enlarge part of it, which
// is how a capture is inspected at the scale an eye actually judges.
// ./gradlew :tools:cropProbe "-PcropArgs=in.png out.png x y w h zoom"
tasks.register<JavaExec>("cropProbe") {
    group = "tools"
    description = "Crop and enlarge part of a PNG, for reviewing a capture by eye."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.muntasimulhaque.crayoner.tools.CropProbeKt"
    args = (project.findProperty("cropArgs") as String? ?: "")
        .split(" ")
        .filter { it.isNotBlank() }
}
