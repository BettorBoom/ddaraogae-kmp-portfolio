plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) apply false
}
// New config is local/Git-ignored. Emulator tests never need a cloud config.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
android {
    namespace = "dev.ddaraogae.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.ddaraogae.app"
        minSdk = 28; targetSdk = 35
        // CI supplies identity without editing source; local previews keep defaults.
        versionCode = providers.gradleProperty("ddaraogae.versionCode").getOrElse("1").toInt()
            .also { require(it in 1..2100000000) { "Invalid ddaraogae.versionCode" } }
        versionName = providers.gradleProperty("ddaraogae.versionName").getOrElse("0.1.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    // Static state captures exclude native window transition frames.
    testOptions { animationsDisabled = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes { release { isDebuggable = false } }
    if (providers.gradleProperty("ddaraogae.scenarios").getOrElse("false").toBoolean()) {
        sourceSets.getByName("debug") {
            java.srcDir("src/scenario/kotlin")
            manifest.srcFile("src/scenario/AndroidManifest.xml")
        }
        sourceSets.getByName("androidTest") { java.srcDir("src/scenarioAndroidTest/kotlin") }
    }
    if (providers.gradleProperty("ddaraogae.firebaseTests").getOrElse("false").toBoolean()) {
        sourceSets.getByName("androidTest") { java.srcDir("src/firebaseAndroidTest/kotlin") }
        sourceSets.getByName("debug") { manifest.srcFile("src/firebaseTest/AndroidManifest.xml") }
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":sharedApp"))
    implementation(project(":domain"))
    implementation(libs.activity.compose)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.5")
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.firebase.auth)
    androidTestImplementation(libs.firebase.firestore)
}
