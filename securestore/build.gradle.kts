plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kover)
}

// Coordinates other builds use to depend on the SDK (includeBuild or Maven): com.guillermocarcelen:securestore
group = "com.guillermocarcelen"

android {
    namespace = "com.guillermocarcelen.securestore"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}
kotlin {
    explicitApi()
}

kover {
    reports {
        filters {
            excludes {
                // Need a real device (Keystore, DataStore files): covered by the instrumented tests
                // in src/androidTest, which Kover does not measure.
                classes(
                    "com.guillermocarcelen.securestore.cypher.AndroidKeyStoreCypher*",
                    "com.guillermocarcelen.securestore.storage.DataStoreStorage*",
                    "com.guillermocarcelen.securestore.SecureStore\$Companion",
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Source: https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.coroutines.test)
}