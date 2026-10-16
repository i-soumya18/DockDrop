plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "in.soumyaranjansahoo.dockdrop"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "in.soumyaranjansahoo.dockdrop"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(project(":spikelab"))
}

abstract class VerifyZeroNetworkPermissionsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFiles: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val bannedPermissions = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE"
        )
        val files = manifestFiles.files
        files.forEach { file ->
            if (file.exists()) {
                val content = file.readText()
                for (banned in bannedPermissions) {
                    if (content.contains(banned)) {
                        throw GradleException("BANNED PERMISSION DETECTED: '$banned' found in ${file.absolutePath}! DockDrop strictly prohibits network access per CLAUDE.md Rule 3.")
                    }
                }
            }
        }
        println("verifyZeroNetworkPermissions passed: No banned network permissions found in ${files.size} manifests.")
    }
}

tasks.register<VerifyZeroNetworkPermissionsTask>("verifyZeroNetworkPermissions") {
    description = "Enforces CLAUDE.md Rule 3: Zero network permissions (fails if INTERNET or ACCESS_NETWORK_STATE is present)."
    group = "verification"
    mustRunAfter(tasks.matching { it.name.contains("Manifest") })
    manifestFiles.from(
        fileTree(layout.buildDirectory.dir("intermediates/merged_manifests")) {
            include("**/*.xml")
        },
        fileTree("src") {
            include("**/AndroidManifest.xml")
        }
    )
}

tasks.named("check") {
    dependsOn("verifyZeroNetworkPermissions")
}