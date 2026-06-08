import org.gradle.api.GradleException
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.snakeladder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.snakeladder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

tasks.register("checkDebugCoverage") {
    group = "verification"
    description = "Fails when debug app logic coverage is below 90% for instruction, line, branch, method, or class."
    dependsOn("createCoverageReport")

    doLast {
        // The raw AGP report still includes these files. The threshold gate excludes
        // Compose UI/platform wrappers where branch counters are dominated by generated
        // recomposition bytecode or device-version branches.
        val thresholdExcludedSourceFiles = setOf(
            "BoardLayers.kt",
            "BoardScreen.kt",
            "CampaignDialog.kt",
            "DifficultyDialog.kt",
            "GameFeedback.kt",
            "LaunchScreen.kt",
            "MainActivity.kt",
            "ProFeatureCatalog.kt",
            "ProFeatureHub.kt",
            "ProgressionHub.kt",
            "ReplayDialog.kt",
            "StoreDialog.kt"
        )
        val reportFile = layout.buildDirectory
            .file("intermediates/code_coverage_data/global/collectDebugCoverage/debugAppAggregatedXmlReport.xml")
            .get()
            .asFile
        if (!reportFile.isFile) {
            throw GradleException("Coverage report was not found: ${reportFile.absolutePath}")
        }

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(reportFile)
        val requiredCounters = listOf("INSTRUCTION", "LINE", "BRANCH", "METHOD", "CLASS")
        val minimum = 0.90

        val totals = requiredCounters.associateWith { 0 to 0 }.toMutableMap()
        val sourceFiles = document.getElementsByTagName("sourcefile")
        for (sourceIndex in 0 until sourceFiles.length) {
            val sourceFile = sourceFiles.item(sourceIndex) as Element
            if (sourceFile.getAttribute("name") in thresholdExcludedSourceFiles) continue

            val counters = sourceFile.getElementsByTagName("counter")
            for (counterIndex in 0 until counters.length) {
                val counter = counters.item(counterIndex) as Element
                val type = counter.getAttribute("type")
                if (type in requiredCounters) {
                    val current = totals.getValue(type)
                    val missed = counter.getAttribute("missed").toInt()
                    val covered = counter.getAttribute("covered").toInt()
                    totals[type] = (current.first + missed) to (current.second + covered)
                }
            }
        }

        val failures = requiredCounters.map { type ->
            val (missed, covered) = totals.getValue(type)
            val total = missed + covered
            val ratio = if (total == 0) 1.0 else covered.toDouble() / total.toDouble()
            val percentage = ratio * 100.0
            logger.lifecycle("$type coverage: ${"%.2f".format(percentage)}% ($covered/$total)")
            type to percentage
        }.filter { (_, percentage) -> percentage < minimum * 100.0 }

        if (failures.isNotEmpty()) {
            val summary = failures.joinToString { (type, percentage) ->
                "$type=${"%.2f".format(percentage)}%"
            }
            throw GradleException("Coverage is below the 90% minimum: $summary")
        }
    }
}

tasks.named("check") {
    dependsOn("checkDebugCoverage")
}
