import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * Release signing comes only from the environment. Nothing secret is committed: if the four
 * NEXCALC_* variables are absent the release build is signed with the CI debug keystore so the APK
 * is installable for review, and the app itself reports which signing kind was used.
 */
val keystorePath: String? = System.getenv("NEXCALC_KEYSTORE_PATH")
val keystorePassword: String? = System.getenv("NEXCALC_KEYSTORE_PASSWORD")
val keyAliasValue: String? = System.getenv("NEXCALC_KEY_ALIAS")
val keyPasswordValue: String? = System.getenv("NEXCALC_KEY_PASSWORD")
val hasReleaseSigning: Boolean = keystorePath != null && keystorePassword != null &&
    keyAliasValue != null && keyPasswordValue != null && file(keystorePath).exists()
val signingKind: String = if (hasReleaseSigning) "release-key" else "debug-key"

android {
    namespace = "com.shohankhan.nexcalc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shohankhan.nexcalc"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("nexcalcRelease") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("nexcalcRelease") else signingConfigs.getByName("debug")
            buildConfigField("String", "SIGNING_KIND", "\"$signingKind\"")
        }
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
            buildConfigField("String", "SIGNING_KIND", "\"debug-key\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "UnusedResources")
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*", "/META-INF/NOTICE*")
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
    }
}

dependencies {
    implementation(project(":core:math"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
