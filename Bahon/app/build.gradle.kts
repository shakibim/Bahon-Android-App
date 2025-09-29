plugins {
    alias(libs.plugins.androidApplication)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bahon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bahon"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation ("com.google.android.gms:play-services-location:19.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore")
    implementation ("org.osmdroid:osmdroid-android:6.1.12")
    implementation ("com.google.zxing:core:3.4.1")
    implementation ("com.google.firebase:firebase-functions")
    implementation ("com.google.firebase:firebase-database")
    // or the latest version
    // ML Kit Barcode Scanning
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")


}