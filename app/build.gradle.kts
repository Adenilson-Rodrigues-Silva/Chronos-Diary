import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}




plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.chronosdiary"
    compileSdk = 36

    buildFeatures {
        buildConfig = true // Adicione esta linha aqui!
    }

    defaultConfig {
        applicationId = "com.example.chronosdiary"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Esta linha lê a chave do seu local.properties e joga para o código
        buildConfigField(
            "String",
            "GOOGLE_API_KEY",
            "\"" + localProperties.getProperty("GOOGLE_API_KEY") + "\""
        )
        packaging {
            resources {
                excludes += "/META-INF/INDEX.LIST"
                excludes += "/META-INF/DEPENDENCIES"
            }


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
    }

    dependencies {
        // Use as referências do Version Catalog (libs) que já estão no seu projeto
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation("androidx.constraintlayout:constraintlayout:2.1.3") // ESTA é a que o XML precisa

        // Mantenha a Biometria (recomendo usar a 1.1.0 estável em vez da alpha para evitar erros)
        implementation("androidx.biometric:biometric:1.1.0")

        // Testes
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)

        // Biblioteca para facilitar o uso do Google Cloud Speech
        implementation("com.google.cloud:google-cloud-speech:4.33.0")


    }

}