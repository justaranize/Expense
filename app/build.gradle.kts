plugins {
    id("com.android.application")
}

android {
    namespace = "com.justaranize.expense"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.justaranize.expense"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
}
