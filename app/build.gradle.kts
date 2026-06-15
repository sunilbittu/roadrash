plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.roadrash.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roadrash.game"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            // Defaults point at the committed demo keystore so `assembleRelease`
            // produces a signed, installable APK out of the box. Override any of
            // these via -P gradle properties or env vars for a real release.
            storeFile = file(
                (project.findProperty("ROADRASH_STORE_FILE") as String?)
                    ?: System.getenv("ROADRASH_STORE_FILE")
                    ?: "roadrash-release.jks"
            )
            storePassword = (project.findProperty("ROADRASH_STORE_PASSWORD") as String?)
                ?: System.getenv("ROADRASH_STORE_PASSWORD") ?: "roadrash"
            keyAlias = (project.findProperty("ROADRASH_KEY_ALIAS") as String?)
                ?: System.getenv("ROADRASH_KEY_ALIAS") ?: "roadrash"
            keyPassword = (project.findProperty("ROADRASH_KEY_PASSWORD") as String?)
                ?: System.getenv("ROADRASH_KEY_PASSWORD") ?: "roadrash"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
