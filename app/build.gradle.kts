plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "ca.mytrails.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ca.mytrails.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Sideloaded personal app: signed with the debug key, no shrinking.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Dependabot owns version updates; these would fail CI on every new release.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
        // targetSdk stays at 36 until its behaviour changes are tested; compileSdk 37 is needed by Compose.
        disable += "OldTargetApi"
        // Backup is deliberately off for this sideloaded app; no launcher icon yet.
        disable += setOf("DataExtractionRules", "MissingApplicationIcon")
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.maplibre.android)
    testImplementation(libs.junit)
}
