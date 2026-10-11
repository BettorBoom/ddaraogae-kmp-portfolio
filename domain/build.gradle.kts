plugins { alias(libs.plugins.kotlin.multiplatform) }
kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.coroutines.core)
        // Local-date math for history-derived stats (ADR-013); already resolved transitively.
        api(libs.datetime)
    }
    sourceSets.commonTest.dependencies { implementation(libs.coroutines.test) }
}
