plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        // Android namespace only (R/BuildConfig); the public Kotlin package stays
        // io.github.tejbhansahu.choreo. Kept distinct from the demo :app's namespace.
        namespace = "io.github.tejbhansahu.choreo.lib"
        compileSdk = 37
        minSdk = 29

        // Enables JVM-side unit tests (commonTest + androidHostTest).
        withHostTestBuilder {
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Choreo"
            isStatic = true
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
