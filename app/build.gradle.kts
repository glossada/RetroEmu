plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "dev.retrotv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.retrotv.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 2
        versionName   = "0.2.0"
    }

    signingConfigs {
        // Firma el release con el debug keystore para poder instalarlo sin Play Store.
        create("debugKey") {
            storeFile        = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword    = "android"
            keyAlias         = "androiddebugkey"
            keyPassword      = "android"
            enableV1Signing  = true   // necesario para boxes con Android < 7
            enableV2Signing  = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig   = signingConfigs.getByName("debugKey")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Incluye arm64-v8a y armeabi-v7a para compatibilidad con boxes 32-bit y 64-bit.
            // Los cores x86/x86_64 viven en src/debug/assets/ y no se incluyen en release.
            ndk {
                abiFilters.add("arm64-v8a")
                abiFilters.add("armeabi-v7a")
            }
        }
        debug {
            // Sin filtro de ABI: permite correr en emuladores x86/x86_64.
            // src/debug/assets/ se fusiona → incluye cores de todos los ABIs.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Opt-in global para las APIs experimentales de tv-material3
        freeCompilerArgs += listOf("-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api")
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.tv.material)
    implementation(libs.libretrodroid)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
}
