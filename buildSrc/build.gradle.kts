plugins {
    `kotlin-dsl`
}

repositories {
    google()
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:builder:7.0.0-alpha03")
    implementation("com.android.tools.build:builder-model:7.0.0-alpha03")
    implementation("com.android.tools.build:gradle:7.0.0-alpha03")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:9.4.1")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
}
