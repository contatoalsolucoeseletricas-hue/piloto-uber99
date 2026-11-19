plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.andreluis.pilotouber99"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.andreluis.pilotouber99"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("RELEASE_STORE_FILE") ?: "release.jks"
            val ksFile = file(ksPath)
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "piloto_release"
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
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
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
}
