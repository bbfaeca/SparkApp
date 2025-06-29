plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sparkapp"
    compileSdk = 35
    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
        }
    }
    defaultConfig {
        applicationId = "com.example.sparkapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gson)
    testImplementation(libs.junit)
    implementation(libs.jsoup)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.hutool.json)
    implementation(libs.okhttp)
}