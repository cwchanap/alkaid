import org.gradle.api.GradleException
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    jacoco
}

android {
    namespace = "com.example.alkaid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alkaid"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    
    // Networking for weather API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Security for API key storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // OSM for alternative map provider
    implementation("org.osmdroid:osmdroid-android:6.1.17")
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

jacoco {
    toolVersion = "0.8.12"
}

val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/android/**/*.*",
    "**/databinding/*",
    "**/*Binding.*",
    "**/*BindingImpl.*",
    "**/DataBinderMapperImpl.*",
    "**/BR.*"
)

val debugClassDirectories = files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude(coverageExclusions)
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude(coverageExclusions)
    }
)

val mainSourceDirectories = files("src/main/java")

val unitCoverageData = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/*.exec")
}

val androidCoverageData = fileTree(layout.buildDirectory) {
    include("outputs/**/*.ec")
}

tasks.register<JacocoReport>("unitCoverageReport") {
    group = "verification"
    description = "Generates JaCoCo coverage reports for debug unit tests."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(debugClassDirectories)
    sourceDirectories.setFrom(mainSourceDirectories)
    additionalSourceDirs.setFrom(mainSourceDirectories)
    executionData.setFrom(unitCoverageData)

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/unitCoverageReport/unitCoverageReport.xml")
        )
        html.required.set(true)
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/jacoco/unitCoverageReport/html")
        )
        csv.required.set(false)
    }

    doFirst {
        if (executionData.files.none { it.exists() }) {
            throw GradleException(
                "No unit coverage data found. Run :app:testDebugUnitTest first."
            )
        }
    }

    doLast {
        println("Unit coverage HTML: ${reports.html.outputLocation.get().asFile.absolutePath}")
        println("Unit coverage XML: ${reports.xml.outputLocation.get().asFile.absolutePath}")
    }
}

tasks.register("androidCoverageReport") {
    group = "verification"
    description = "Runs debug instrumentation coverage and generates the Android coverage report."
    dependsOn("connectedDebugAndroidTest", "createDebugAndroidTestCoverageReport")

    doLast {
        println(
            "Android coverage report generated under: " +
                layout.buildDirectory.dir("reports/coverage/androidTest/debug").get().asFile.absolutePath
        )
    }
}

tasks.register<JacocoReport>("combinedCoverageReport") {
    group = "verification"
    description = "Generates a merged JaCoCo report from debug unit and instrumentation coverage."
    dependsOn("unitCoverageReport", "androidCoverageReport")

    classDirectories.setFrom(debugClassDirectories)
    sourceDirectories.setFrom(mainSourceDirectories)
    additionalSourceDirs.setFrom(mainSourceDirectories)
    executionData.setFrom(unitCoverageData, androidCoverageData)

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/combinedCoverageReport/combinedCoverageReport.xml")
        )
        html.required.set(true)
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/jacoco/combinedCoverageReport/html")
        )
        csv.required.set(false)
    }

    doFirst {
        if (executionData.files.none { it.exists() }) {
            throw GradleException(
                "No coverage execution data found. Run :app:unitCoverageReport and :app:androidCoverageReport first."
            )
        }
    }

    doLast {
        println("Combined coverage HTML: ${reports.html.outputLocation.get().asFile.absolutePath}")
        println("Combined coverage XML: ${reports.xml.outputLocation.get().asFile.absolutePath}")
    }
}

tasks.register("coverageSummary") {
    group = "verification"
    description = "Prints coverage percentages from the combined report when available."
    dependsOn("unitCoverageReport")

    doLast {
        val combinedXml = layout.buildDirectory
            .file("reports/jacoco/combinedCoverageReport/combinedCoverageReport.xml")
            .get()
            .asFile
        val unitXml = layout.buildDirectory
            .file("reports/jacoco/unitCoverageReport/unitCoverageReport.xml")
            .get()
            .asFile

        val reportFile = when {
            combinedXml.exists() -> combinedXml
            unitXml.exists() -> {
                println("Combined coverage XML not found; using unit coverage report only.")
                unitXml
            }
            else -> throw GradleException(
                "No coverage XML report found. Run :app:unitCoverageReport or :app:combinedCoverageReport first."
            )
        }

        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = documentBuilderFactory
            .newDocumentBuilder()
            .apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
            }
            .parse(reportFile)
        val counters = document.getElementsByTagName("counter")

        fun printCounterSummary(type: String) {
            for (index in 0 until counters.length) {
                val node = counters.item(index)
                if (node.parentNode.nodeName != "report") continue
                val attributes = node.attributes
                if (attributes.getNamedItem("type")?.nodeValue != type) continue

                val covered = attributes.getNamedItem("covered").nodeValue.toDouble()
                val missed = attributes.getNamedItem("missed").nodeValue.toDouble()
                val total = covered + missed
                val percent = if (total == 0.0) 0.0 else covered / total * 100.0
                println(
                    String.format(
                        Locale.US,
                        "%s coverage: %.2f%% (%d/%d)",
                        type.lowercase().replaceFirstChar { it.uppercase() },
                        percent,
                        covered.toInt(),
                        total.toInt()
                    )
                )
                return
            }
        }

        printCounterSummary("INSTRUCTION")
        printCounterSummary("LINE")
        printCounterSummary("BRANCH")
    }
}
