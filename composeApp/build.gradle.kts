import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.okhttp)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment)
            implementation(libs.ktor.client.android)
            
            // Firebase (Android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "com.vasmarfas.smsnode"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.vasmarfas.smsnode"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.vasmarfas.smsnode.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg,TargetFormat.Pkg, TargetFormat.Msi,  TargetFormat.Exe, TargetFormat.Deb)
            packageName = "SMSNode"
            packageVersion = System.getenv("APP_VERSION_NAME") ?: "1.0.0"

            macOS {
                iconFile.set(project.file("icons/logo.png"))
                bundleID = "com.vasmarfas.smsnode"
                
                val isAppStoreRelease = project.hasProperty("macOsAppStoreRelease")
                appStore = isAppStoreRelease
                
                val buildVersion = System.getenv("APP_VERSION_CODE")
                if (buildVersion != null && buildVersion.isNotBlank()) {
                    packageBuildVersion = buildVersion
                }
                
                val identity = System.getenv("APPLE_DEVELOPER_ID_IDENTITY")
                if (!identity.isNullOrBlank()) {
                    signing {
                        sign.set(true)
                        this.identity.set(identity)
                    }
                }
                
                if (isAppStoreRelease) {
                    appStore = true
                    // Файлы для Mac App Store (вам нужно будет их создать и положить в корень composeApp)
                    val provFile = project.file("embedded.provisionprofile")
                    if (provFile.exists()) provisioningProfile.set(provFile)
                    
                    val runtimeProvFile = project.file("runtime.provisionprofile")
                    if (runtimeProvFile.exists()) runtimeProvisioningProfile.set(runtimeProvFile)
                    
                    val entFile = project.file("entitlements.plist")
                    if (entFile.exists()) entitlementsFile.set(entFile)
                    
                    val runtimeEntFile = project.file("runtime-entitlements.plist")
                    if (runtimeEntFile.exists()) runtimeEntitlementsFile.set(runtimeEntFile)
                }

                val appleId = System.getenv("APPLE_ID")
                if (!appleId.isNullOrBlank()) {
                    notarization {
                        appleID.set(appleId)
                        password.set(System.getenv("APPLE_ID_PASSWORD"))
                        teamID.set(System.getenv("APPLE_TEAM_ID"))
                    }
                }
            }
            windows {
                iconFile.set(project.file("icons/logo.ico"))
            }
            linux {
                iconFile.set(project.file("icons/logo.png"))
            }
        }
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    freeArgs.add("--verbose")
}

