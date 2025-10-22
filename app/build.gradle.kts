plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.gms.google.services)
}
kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.example.ask"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ask"
        minSdk = 26
        targetSdk = 36
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
    buildFeatures{
        dataBinding=true
        viewBinding=true
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    //toast
    implementation (libs.motiontoast)
    //gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation (libs.androidx.swiperefreshlayout)
    implementation(libs.glide)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.circleimageview)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Check for latest version
    implementation(libs.androidx.lifecycle.livedata.ktx) // For LiveData if preferred
    implementation(libs.androidx.lifecycle.runtime.ktx) // For lifecycleScope
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    kapt("com.github.bumptech.glide:compiler:4.16.0")


}