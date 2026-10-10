plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}
kotlin {
    android { namespace = "dev.ddaraogae.dogs"; compileSdk = 36; minSdk = 28 }
    sourceSets.commonMain.dependencies {
        implementation(project(":domain"))
        implementation(project(":designsystem"))
        implementation(compose.foundation)
    }
}
