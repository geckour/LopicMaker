import com.android.build.gradle.internal.packaging.getDefaultDebugKeystoreLocation

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("io.fabric")
    id("com.google.gms.google-services") apply false
}

android {
    compileSdkVersion(Deps.GradlePlugin.compileSdkVersion)
    defaultConfig {
        applicationId = "com.geckour.lopicmaker"
        minSdkVersion(Deps.GradlePlugin.minSdkVersion)
        targetSdkVersion(Deps.GradlePlugin.targetSdkVersion)
        versionCode = 2
        versionName = "1.0.1"
        testInstrumentationRunner = Deps.Test.instrumentTestRunner

        dataBinding.isEnabled = true
    }
    signingConfigs {
        getByName("debug") {
            storeFile = getDefaultDebugKeystoreLocation()
        }
        create("release") {
            val releaseSettingGradleFile = File("${project.rootDir}/app/signing/release.gradle")
            if (releaseSettingGradleFile.exists())
                apply(from = releaseSettingGradleFile, to = android)
            else
                throw GradleException("Missing ${releaseSettingGradleFile.absolutePath} . Generate the file by copying and modifying ${project.rootDir}/app/signing/release.gradle.sample .")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    packagingOptions {
        exclude("META-INF/license.txt")
        exclude("META-INF/notice.txt")
    }
    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.1")
    }
    lintOptions {
        disable("ClickableViewAccessibility")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(Deps.Kotlin.stdlib)
    implementation(Deps.AndroidX.appCompat)
    implementation(Deps.AndroidX.coreKtx)
    implementation(Deps.AndroidX.design)
    implementation(Deps.AndroidX.constraint)
    testImplementation(Deps.Test.junit)
    androidTestImplementation(Deps.Test.testRunner)
    androidTestImplementation(Deps.Test.espressoCore)

    // Coroutines
    implementation(Deps.Kotlin.Coroutines.core)
    implementation(Deps.Kotlin.Coroutines.android)

    // Firebase
    implementation(Deps.Firebase.core)
    implementation(Deps.Firebase.crashlytics) { isTransitive = true }

    // Logging
    implementation(Deps.Timber.timber)

    // Inspect
    implementation(Deps.Stetho.stetho)
    implementation(Deps.Stetho.okhttp)

    // Gson
    implementation(Deps.Gson.gson)

    // ViewModel
    implementation(Deps.AndroidX.Lifecycle.extensions)
    implementation(Deps.AndroidX.Lifecycle.viewModelKtx)
    kapt(Deps.AndroidX.Lifecycle.compiler)

    // Room
    implementation(Deps.AndroidX.Room.runtime)
    kapt(Deps.AndroidX.Room.compiler)
    implementation(Deps.AndroidX.Room.coroutines)

    // SAT solver
    implementation(Deps.SAT4J.sat4J)
}

apply(plugin = "com.google.gms.google-services")
