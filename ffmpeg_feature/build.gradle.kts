plugins {
    alias(libs.plugins.android.dynamic.feature)
}
android {
    namespace = "de.timdavidfriedrich.av_converter.ffmpeg_feature"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs {
            // This forces Android to extract native libraries from the APK to disk
            // upon installation. This bypasses the 16KB alignment requirement
            // for zipped libs (which causes the crash).
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Ffmpeg for legacy formats like MJPEG AVI
    implementation(files("libs/ffmpeg-kit-full-gpl-6.0-2.LTS.aar"))
    // Necessary for ffmpeg
    implementation(files("libs/smart-exception-java-0.2.1.jar"))
    implementation(files("libs/smart-exception-common-0.2.1.jar"))
}