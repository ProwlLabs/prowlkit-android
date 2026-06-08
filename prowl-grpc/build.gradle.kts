plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.prowllabs.prowl.grpc"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=com.prowllabs.prowl.core.ProwlInternalApi",
        )
    }
}

dependencies {
    api(project(":prowl-core"))
    implementation(libs.grpc.api)
    implementation(libs.grpc.stub)
}
