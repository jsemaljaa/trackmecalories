// Code shared by the Android app and the (planned) iOS app: domain math, the Room schema and
// DAO, the food sources (bundled lists, Open Food Facts, Kalorické tabulky), and the Compose UI
// with its strings and fonts (composeResources). Nothing here may use Android or JVM-only APIs;
// platform pieces go through expect/actual in androidMain / iosMain.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    android {
        namespace = "com.jsemalja.trackmecalories.shared"
        compileSdk = 36
        minSdk = 26
        androidResources.enable = true
    }
    // iOS targets only compile on a Mac (or a macOS CI runner); on Windows they're skipped.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room.runtime)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.ui.tooling.preview)
            api(libs.cmp.ui.backhandler)
            api(libs.cmp.resources)
            api(libs.cmp.material3)
            api(libs.cmp.material3.navigation.suite)
            api(libs.cmp.material.icons.extended)
            api(libs.cmp.lifecycle.viewmodel.compose)
            api(libs.cmp.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

compose.resources {
    // The app module still uses Res while screens move over.
    publicResClass = true
    packageOfResClass = "com.jsemalja.trackmecalories.resources"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
