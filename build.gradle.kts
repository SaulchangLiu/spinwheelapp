// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.6.0") // ✅ Kotlin DSL syntax
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        //classpath("org.jetbrains.kotlin:kotlin-compiler-plugin-embeddable:2.0.0")
    }
}