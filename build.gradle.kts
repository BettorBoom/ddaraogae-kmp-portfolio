import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.kmp) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// ADR-006: fixtures are opt-in compile inputs, never a hidden release menu.
val scenariosEnabled = providers.gradleProperty("ddaraogae.scenarios").map(String::toBoolean).getOrElse(false)
val firebaseTestsEnabled = providers.gradleProperty("ddaraogae.firebaseTests").map(String::toBoolean).getOrElse(false)
allprojects {
    tasks.configureEach {
        if (name.contains("Release", ignoreCase = true)) {
            doFirst {
                check(!scenariosEnabled) { "DDARAOGAE_SCENARIOS_RELEASE_FORBIDDEN: omit ddaraogae.scenarios for release" }
                check(!firebaseTestsEnabled) { "DDARAOGAE_FIREBASE_TESTS_RELEASE_FORBIDDEN: omit ddaraogae.firebaseTests for release" }
            }
        }
    }
}

// Sync-folder copies of generated .class files break D8. Keep this machine's
// build output outside Documents, configured locally rather than in source.
val localSettings = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
localSettings.getProperty("ddaraogae.buildRoot")?.let { outputRoot ->
    allprojects {
        layout.buildDirectory.set(file("$outputRoot/${path.trim(':').replace(':', '/').ifEmpty { "root" }}"))
    }
}

// KMP modules share these targets. Extract a convention plugin when
// configuration diverges; keep the toolchain decision in ADR-004.
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(17)
            jvm()
            iosArm64()
            iosSimulatorArm64()
            sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
        }
    }
}

tasks.register<Exec>("architectureCheck") {
    commandLine("python3", "scripts/architecture_check.py")
}
