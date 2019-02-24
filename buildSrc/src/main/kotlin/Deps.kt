object Deps {
    object Kotlin {
        const val version = "1.3.21"
        val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"

        object Coroutines {
            const val version = "1.0.1"
            val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        }
    }

    object GradlePlugin {
        const val android = "com.android.tools.build:gradle:3.3.1"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Kotlin.version}"
        const val googleService = "com.google.gms:google-services:4.0.1"
        const val fabric = "io.fabric.tools:gradle:1.26.1"

        const val compileSdkVersion = 28
        const val minSdkVersion = 21
        const val targetSdkVersion = 28
    }

    object Test {
        const val junit = "junit:junit:4.12"
        const val testRunner = "androidx.test:runner:1.1.2-alpha01"
        const val instrumentTestRunner = "androidx.test.runner.AndroidJUnitRunner"
        const val espressoCore = "androidx.test.espresso:espresso-core:3.1.2-alpha01"
    }

    object AndroidX {
        const val appCompat = "androidx.appcompat:appcompat:1.1.0-alpha02"
        const val coreKtx = "androidx.core:core-ktx:1.1.0-alpha04"
        const val design = "com.google.android.material:material:1.1.0-alpha03"
        const val constraint = "androidx.constraintlayout:constraintlayout:2.0.0-alpha3"

        object Lifecycle {
            const val version = "2.1.0-alpha02"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
        }

        object Room {
            const val version = "2.1.0-alpha04"
            const val runtime = "androidx.room:room-runtime:$version"
            const val compiler = "androidx.room:room-compiler:$version"
            const val coroutines = "androidx.room:room-coroutines:$version"
        }
    }

    object Timber {
        const val timber = "com.jakewharton.timber:timber:4.7.1"
    }

    object Stetho {
        const val version = "1.5.0"
        const val stetho = "com.facebook.stetho:stetho:$version"
        const val okhttp = "com.facebook.stetho:stetho-okhttp:$version"
    }

    object Firebase {
        const val core = "com.google.firebase:firebase-core:16.0.4"
        const val crashlytics = "com.crashlytics.sdk.android:crashlytics:2.9.3"
    }

    object Gson {
        const val gson = "com.google.code.gson:gson:2.8.5"
    }

    object SAT4J {
        const val sat4J = "org.ow2.sat4j:org.ow2.sat4j.core:2.3.5"
    }
}