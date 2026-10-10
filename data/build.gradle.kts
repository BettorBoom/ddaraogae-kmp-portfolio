plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}
kotlin {
    android {
        namespace = "dev.ddaraogae.data"; compileSdk = 36; minSdk = 28
        withJava()
    }
    sourceSets.commonMain.dependencies {
        api(project(":domain"))
        implementation(libs.room.runtime)
        implementation(libs.sqlite.bundled)
    }
    sourceSets.commonTest.dependencies { implementation(libs.coroutines.test) }
    sourceSets.androidMain.dependencies {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.firebase.auth)
        implementation(libs.firebase.firestore)
    }
    // ponytail: KSP/AGP 8.13 misses the generated Kotlin actual constructor root;
    // remove this explicit root when a verified plugin update wires it itself.
    sourceSets.androidMain { kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/android/androidMain/kotlin")) }
}
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
room { schemaDirectory("$projectDir/schemas") }
