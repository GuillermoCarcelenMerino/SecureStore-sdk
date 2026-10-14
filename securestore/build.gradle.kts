plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.vanniktech.publish)
}

// Coordinates other builds use to depend on the SDK (includeBuild or Maven): io.github.guillermocarcelenmerino:securestore
group = "io.github.guillermocarcelenmerino"
version = "0.1.0"


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

// Publishing to Maven Central. Credentials and the GPG key are read from the user's
// ~/.gradle/gradle.properties (mavenCentralUsername/Password, signingInMemoryKey*), never from the repo.
mavenPublishing {
    coordinates(group.toString(), "securestore", version.toString())
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("SecureStore")
        description.set("Encrypted key-value storage for Android, backed by the Android Keystore.")
        inceptionYear.set("2026")
        url.set("https://github.com/GuillermoCarcelenMerino/SecureStore-sdk")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("GuillermoCarcelenMerino")
                name.set("Guillermo Carcelén")
                url.set("https://github.com/GuillermoCarcelenMerino")
            }
        }
        scm {
            url.set("https://github.com/GuillermoCarcelenMerino/SecureStore-sdk")
            connection.set("scm:git:git://github.com/GuillermoCarcelenMerino/SecureStore-sdk.git")
            developerConnection.set("scm:git:ssh://git@github.com/GuillermoCarcelenMerino/SecureStore-sdk.git")
        }
    }
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