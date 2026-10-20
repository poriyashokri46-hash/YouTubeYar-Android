import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// Release signing values are supplied only by the release-artifacts CI workflow.
// Never commit keystores or signing passwords to the repository.
val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank()

// The approved launcher artwork is stored as base64 text so the repository can
// generate proper binary Android resources reproducibly during every build.
val launcherResourcesDir = layout.buildDirectory.dir("generated/res/launcherIcons")
val generateLauncherIconResources by tasks.registering {
    val fullIcon = layout.projectDirectory.file("launcher-assets/ic_launcher_full.jpg.b64")
    inputs.file(fullIcon)
    outputs.dir(launcherResourcesDir)
    doLast {
        val outputRoot = launcherResourcesDir.get().asFile
        val fullTarget = File(outputRoot, "mipmap-nodpi/ic_launcher_approved.jpg")
        val roundTarget = File(outputRoot, "mipmap-nodpi/ic_launcher_approved_round.jpg")
        val adaptiveTarget = File(outputRoot, "drawable-nodpi/ic_launcher_approved_artwork.jpg")
        fullTarget.parentFile.mkdirs()
        adaptiveTarget.parentFile.mkdirs()
        val imageBytes = Base64.getDecoder().decode(fullIcon.asFile.readText().trim())
        fullTarget.writeBytes(imageBytes)
        roundTarget.writeBytes(imageBytes)

        val source = ImageIO.read(ByteArrayInputStream(imageBytes))
        val interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC
        val densitySizes = linkedMapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
        densitySizes.forEach { (density, size) ->
            val densityDir = File(outputRoot, "mipmap-$density")
            densityDir.mkdirs()
            val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
            val graphics = resized.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation)
            graphics.drawImage(source, 0, 0, size, size, null)
            graphics.dispose()
            ImageIO.write(resized, "png", File(densityDir, "ic_launcher_approved.png"))
            ImageIO.write(resized, "png", File(densityDir, "ic_launcher_approved_round.png"))
        }

        // Keep the complete artwork inside Android's adaptive-icon safe zone.
        val safeSize = 128
        val canvas = BufferedImage(192, 192, BufferedImage.TYPE_INT_RGB)
        val graphics = canvas.createGraphics()
        graphics.color = Color.BLACK
        graphics.fillRect(0, 0, 192, 192)
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation)
        graphics.drawImage(source, (192 - safeSize) / 2, (192 - safeSize) / 2, safeSize, safeSize, null)
        graphics.dispose()
        ImageIO.write(canvas, "jpg", adaptiveTarget)

        val adaptiveXml = listOf(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
            "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">",
            "    <background android:drawable=\"@color/launcher_icon_black\" />",
            "    <foreground android:drawable=\"@drawable/ic_launcher_approved_artwork\" />",
            "</adaptive-icon>"
        ).joinToString("\n")
        val adaptiveDir = File(outputRoot, "mipmap-anydpi-v26")
        adaptiveDir.mkdirs()
        File(adaptiveDir, "ic_launcher_approved.xml").writeText(adaptiveXml)
        File(adaptiveDir, "ic_launcher_approved_round.xml").writeText(adaptiveXml)
    }
}
android {
    sourceSets.getByName("main").res.srcDir(launcherResourcesDir)
    namespace = "com.youtubeyar.app"
    compileSdk = 36

    defaultConfig {
        // Tapsell's app key is packaged into the client APK, so it is not a server-side secret.
        // Keep it out of source control and inject it from GitHub Actions for release builds.
        resValue("string", "tapsell_app_key", System.getenv("TAPSELL_APP_KEY") ?: "")
        // Normalize PEM formatting before generating an Android string resource:
        // XML resources must not contain raw PEM headers or embedded line breaks.
        val bazaarPublicKey = (System.getenv("BAZAAR_RSA_PUBLIC_KEY") ?: "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .filterNot { it.isWhitespace() }
        resValue("string", "bazaar_rsa_public_key", bazaarPublicKey)
        // Optional CI-provided YouTube key; runtime user-entered key remains supported.
        resValue("string", "youtube_api_key", System.getenv("YOUTUBE_API_KEY") ?: "")
        applicationId = "com.youtubeyar.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // PR validation workflows do not receive repository signing secrets.
            // Use debug signing only for those validation builds; the dedicated
            // release-artifacts workflow provides the private release keystore.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        resources.excludes += "/META-INF/DEPENDENCIES"
    }
    lint { abortOnError = true }
}

tasks.configureEach {
    val needsLauncherResources =
        (name.startsWith("generate") && name.endsWith("Resources")) ||
        (name.startsWith("map") && name.endsWith("SourceSetPaths")) ||
        name.startsWith("extractDeepLinks") ||
        (name.startsWith("merge") && name.endsWith("Resources"))
    if (name != "generateLauncherIconResources" && needsLauncherResources) {
        dependsOn(generateLauncherIconResources)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Network (YouTube Data API + optional online AI)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // WorkManager (daily reminders)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Tapsell SDK (ads)
    implementation("ir.tapsell.sdk:tapsell-sdk-android:4.9.16")
}
