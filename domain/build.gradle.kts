plugins { alias(libs.plugins.kotlin.multiplatform) }
kotlin {
    sourceSets.commonMain.dependencies { api(libs.coroutines.core) }
    sourceSets.commonTest.dependencies { implementation(libs.coroutines.test) }
}
