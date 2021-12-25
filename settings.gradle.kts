buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
            name = "ossrh-snapshot"
        }
        maven(url = "https://maven.minecraftforge.net/") {
            name = "forge"
        }
    }
    dependencies {
        // use latest version by dependabot. dependabot supports dependencies in settings.gradle.kts
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.26")
    }
}

rootProject.name = "anti-raid-weapons"
