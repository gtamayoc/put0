import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.sonarqube") version "7.2.2.6593"
}

fun versionCodeFrom(semantic: String): Int {
    val (major, minor, patch) = semantic.split(".").map { it.toInt() }
    return major * 10000 + minor * 100 + patch
}

val semanticVersion = "1.0.0"
val baseVersionCode = versionCodeFrom(semanticVersion)

android {
    namespace = "gtc.dcc.put0"
    compileSdk = 34

    defaultConfig {
        applicationId = "gtc.dcc.put0"
        minSdk = 23
        targetSdk = 34
        versionName = semanticVersion
        versionCode = baseVersionCode

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    /* =====================
       SIGNING (CI / LOCAL)
       ===================== */

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()

    android {

        signingConfigs {
            create("release") {
                val keystorePath = System.getenv("KEYSTORE_FILE")

                if (!keystorePath.isNullOrBlank()) {
                    storeFile = rootProject.file(keystorePath)
                    storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                    keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                    keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
                }
            }
        }

        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(project(":game-core"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    implementation(libs.recyclerview)
    implementation(libs.core.animation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ViewModel y LiveData
    implementation (libs.lifecycle.viewmodel.ktx)
    implementation (libs.lifecycle.livedata.ktx)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.analytics)

    //LOGS
    implementation(libs.logger)

    // Import the BoM for the Firebase platform
    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.auth)

    // Also add the dependency for the Google Play services library and specify its version
    implementation(libs.play.services.auth)

    // Declare the dependency for the Cloud Firestore library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation(libs.firebase.firestore)

    //Gson
    implementation(libs.gson)
    implementation(libs.google.gson)

    //Security for SharedPreference
    implementation(libs.security.crypto)

    //Library to be able to implement images
    implementation(libs.glide)

    //Library to be able to implement circleimageview
    implementation (libs.circleimageview)

    implementation (libs.cardview)
    implementation (libs.constraintlayout.v214)

    implementation (libs.bottomsheets)
    implementation (libs.core)

    // Networking - Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson.v290)
    implementation(libs.logging.interceptor)

    // WebSocket - Stomp
    implementation(libs.stompprotocolandroid)
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.rxjava3)
    implementation(libs.rxandroid3)

    // Pure Java Logic & Utils
    implementation(libs.guava)
    implementation(libs.squirrel.foundation)

    // UI Animations
    implementation(libs.lottie)
    
    // LeakCanary
    debugImplementation(libs.leakcanary)
}