plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.muntasimulhaque.crayoner"
    compileSdk = 37

    defaultConfig {
        // Must never change: this is the Play Store package ID, the
        // ninetynine and puzzlet convention (io.github.muntasimulhaque.*).
        applicationId = "io.github.muntasimulhaque.crayoner"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2"

        // The emulator screenshot capture uses AndroidX's runner.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Provided by CI (see .github/workflows/build.yml). Local builds
            // without the keystore fall back to an unsigned release build.
            val ksFile = file(System.getenv("KEYSTORE_FILE") ?: "signing.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 code shrinking + resource shrinking. Safe here: no
            // reflection, serialization, or JNI, only framework APIs (the
            // insets controller) and Compose, both of which ship their own
            // keep rules.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val ks = signingConfigs.getByName("release")
            if (ks.storeFile?.exists() == true) {
                signingConfig = ks
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Full lint runs in CI next to the unit tests (:app:lintRelease).
        abortOnError = true
        checkDependencies = false
    }
}

dependencies {
    // The rules: pure Kotlin, shared with the offline generators in :tools.
    implementation(project(":core"))
    // The Compose BOM governs every Compose artifact. The dependency set is
    // deliberately minimal; each new one is proposed in AGENTS.md first.
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // One small preference file: the sound switch, finished pictures, and
    // the colors of a picture still being worked on.
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")

    // Instrumented (emulator) screenshot capture: a bare ComponentActivity
    // hosts each state and PixelCopy grabs the window. No compose test rule,
    // no Espresso, no injection machinery: rendering states and copying
    // pixels needs none of it, so captures keep working on whatever image
    // the app targets.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
