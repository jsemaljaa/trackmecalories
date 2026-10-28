// Code shared by the Android app and the (planned) iOS app: domain math, the Room schema and
// DAO, and parsing of the bundled food lists. Nothing here may use Android or JVM-only APIs;
// platform pieces go through expect/actual in androidMain / iosMain.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
}

kotlin {
    android {
        namespace = "com.jsemalja.trackmecalories.shared"
        compileSdk = 36
        minSdk = 26
    }
    // iOS targets only compile on a Mac (or a macOS CI runner); on Windows they're skipped.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room.runtime)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
