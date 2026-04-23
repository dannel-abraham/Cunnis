plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// Read version from version.properties (managed by CI/CD)
val versionPropsFile = rootProject.file("version.properties")
val versionProps = java.util.Properties()
if (versionPropsFile.exists()) {
    versionProps.load(versionPropsFile.inputStream())
}
val appVersionCode = (versionProps.getProperty("VERSION_CODE", "1") as String).toInt()
val appVersionName = versionProps.getProperty("VERSION_NAME", "0.1.0") as String

android {
    namespace = "cu.dandroid.cunnis"
    compileSdk = 34

    defaultConfig {
        applicationId = "cu.dandroid.cunnis"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // CI pasa KEYSTORE_PATH=release-key.jks (raíz del repo)
            // Build local usa ../release-key.jks (app/ → raíz)
            val ksPath = System.getenv("KEYSTORE_PATH")
            storeFile = if (ksPath != null) {
                file("../${ksPath}")
            } else {
                file("../release-key.jks")
            }
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "dannel@alvarez"
            keyAlias = System.getenv("KEY_ALIAS") ?: "dannel"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "dannel@alvarez"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "Cunnis-${variant.versionName}-${variant.name}.apk"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.common.java8)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.workmanager)
    implementation(libs.preference)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.viewpager2)
    implementation(libs.swiperefreshlayout)
    implementation(libs.mpandroidchart)
    implementation(libs.circleimageview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}
