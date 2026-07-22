import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        getByName("androidInstrumentedTest").dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
        }
    }
}

android {
    namespace = "com.biscuitbag"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.biscuitbag"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        disable += "NullSafeMutableLiveData"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("BiscuitBagDatabase") {
            packageName.set("com.biscuitbag.database")
        }
    }
}

// SQLDelight 迁移文件目录：composeApp/src/commonMain/sqldelight/migrations/
// 表结构变更时，在该目录下创建 1.sqm、2.sqm 等迁移文件
