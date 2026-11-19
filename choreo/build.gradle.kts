import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.maven.publish)
}

group = "io.github.tejbhansahu"
version = "0.1.0"

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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates(group.toString(), "choreo", version.toString())

    pom {
        name.set("Choreo")
        description.set("Adaptive, frame-budgeted work scheduling for Kotlin Multiplatform (Android & iOS).")
        inceptionYear.set("2026")
        url.set("https://github.com/tejbhansahu/Choreo")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("tejbhansahu")
                name.set("Tejbhan Sahu")
                url.set("https://github.com/tejbhansahu")
            }
        }
        scm {
            url.set("https://github.com/tejbhansahu/Choreo")
            connection.set("scm:git:git://github.com/tejbhansahu/Choreo.git")
            developerConnection.set("scm:git:ssh://git@github.com/tejbhansahu/Choreo.git")
        }
    }
}
