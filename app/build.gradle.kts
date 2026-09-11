plugins {
    id("com.android.application")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.keepers.photoorganiser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.keepers.photoorganiser"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-poc"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("com.google.guava:guava:33.7.1-android")
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.android.gms:play-services-auth:21.6.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
