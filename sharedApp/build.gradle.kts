plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}
kotlin {
    android { namespace = "dev.ddaraogae.shared"; compileSdk = 36; minSdk = 28 }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "SharedApp"
            isStatic = true
            export(project(":domain"))
            export(project(":data"))
            binaryOption("bundleId", "dev.ddaraogae.shared")
        }
    }
    sourceSets.commonMain.dependencies {
        api(project(":domain"))
        api(project(":data"))
        implementation(project(":designsystem"))
        implementation(project(":feature:walk"))
        implementation(project(":feature:dogs"))
        implementation(project(":feature:history"))
        implementation(project(":feature:mypage"))
        implementation(project(":feature:stamp"))
        implementation(project(":feature:auth"))
        implementation(compose.foundation)
        implementation(libs.navigation.ui)
        implementation(libs.lifecycle.navigation)
        implementation(libs.serialization.json)
        implementation(libs.koin.core)
    }
    if (providers.gradleProperty("ddaraogae.scenarios").getOrElse("false").toBoolean()) {
        sourceSets.commonMain { kotlin.srcDir("src/scenarioMain/kotlin") }
        sourceSets.iosMain { kotlin.srcDir("src/scenarioIos/kotlin") }
    }
    sourceSets.commonTest.dependencies { implementation(libs.coroutines.test) }
}
