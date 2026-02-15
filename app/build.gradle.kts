import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.example.chronosdiary"
    compileSdk = 36 // Reduzi para 35 pois o 36 ainda é muito experimental e pode dar erro de biblioteca

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.chronosdiary"
        minSdk = 25
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GOOGLE_API_KEY",
            "\"" + localProperties.getProperty("GOOGLE_API_KEY") + "\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

// AQUI É O ÚNICO LUGAR DAS DEPENDÊNCIAS (FORA DO BLOCO ANDROID)
dependencies {
    // Core e UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Biometria
    implementation("androidx.biometric:biometric:1.1.0")

    // Lottie (Animações)
    implementation("com.airbnb.android:lottie:6.1.0")

    // Google Cloud Speech
    implementation("com.google.cloud:google-cloud-speech:4.33.0")

    // Room (Banco de Dados)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}