import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

val appVersionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
val semver = appVersionName.removePrefix("v").split(".")
val appVersionCode = semver.getOrNull(0)?.toIntOrNull().orZero() * 10000 +
    semver.getOrNull(1)?.toIntOrNull().orZero() * 100 +
    semver.getOrNull(2)?.toIntOrNull().orZero()

fun Int?.orZero(): Int = this ?: 0

// Local release signing: keystore.properties lives outside version control.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val signingStoreFile = (System.getenv("KEYSTORE_FILE") ?: keystoreProps.getProperty("storeFile"))
    ?.let { file(it) }
val signingReady = signingStoreFile != null && signingStoreFile.exists()

android {
    namespace = "com.vitrum.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vitrum.tv"
        minSdk = 22
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName.removePrefix("v")
        resourceConfigurations += listOf("en", "it")

        buildConfigField("String", "GITHUB_OWNER", "\"danyx67800\"")
        buildConfigField("String", "GITHUB_REPO", "\"Vitrum\"")
        buildConfigField("String", "UPDATE_API", "\"https://api.github.com/\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingReady) {
            create("release") {
                storeFile = signingStoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingReady) signingConfig = signingConfigs.getByName("release")
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
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES")
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        lintConfig = file("lint.xml")
        disable += setOf("GoogleAppIndexingWarning")
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.zxing.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
