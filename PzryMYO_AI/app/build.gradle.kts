import java.util.Properties

plugins {
    id("com.android.application")
    // DÜZELTME BURADA: Versiyonları 2.0.21 yaptık (Sistemdekiyle eşleşti)
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.pzrymyo_ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pzrymyo_ai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
            // --- SİHİRLİ DOKUNUŞ BURADA / MAGIC TOUCH HERE ---

            // 1. Önce değeri alıyoruz / First we get the value
            val rawApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""

            // 2. İçindeki tırnak işaretlerini (") zorla siliyoruz
            // 2. We forcibly delete any quote marks (") inside it
            val cleanApiKey = rawApiKey.replace("\"", "")

            // 3. Sonra kodun içine biz kendi ellerimizle temiz tırnak ekliyoruz
            // 3. Then we add clean quotes into the code with our own hands
            buildConfigField("String", "GEMINI_API_KEY", "\"$cleanApiKey\"")
        }

        val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        //buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // Kotlin 2.0+ kullandığımız için burası boş kalabilir, plugin hallediyor.
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation(libs.firebase.firestore)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}