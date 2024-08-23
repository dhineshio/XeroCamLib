buildscript {
  val kotlin_version by extra("1.7.20")

  repositories {
    google()
    mavenCentral()
    mavenLocal()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:7.1.3")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
  }
}
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  id("maven-publish")
}

android {
  namespace = "com.xero.xerocamera"
  compileSdk = 34

  defaultConfig {
    minSdk = 26

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      consumerProguardFiles ("consumer-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)       /// << --- ADD This
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17          ////  << --- ADD This
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  val camerax_version = "1.4.0-beta01"
  implementation("androidx.camera:camera-core:${camerax_version}")
  implementation("androidx.camera:camera-camera2:${camerax_version}")
  implementation("androidx.camera:camera-lifecycle:${camerax_version}")
  implementation("androidx.camera:camera-video:${camerax_version}")
  implementation("androidx.camera:camera-view:${camerax_version}")
  implementation("androidx.camera:camera-mlkit-vision:${camerax_version}")
  implementation("androidx.camera:camera-extensions:${camerax_version}")

  implementation(libs.play.services.mlkit.barcode.scanning)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "com.github.dhineshio"
      artifactId = "XeroCamLib"
      version = "1.0"
      pom {
        description.set("Xero Camera Library")
      }
    }
  }
  repositories {
    mavenLocal()
  }
}