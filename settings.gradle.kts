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
        classpath("com.anatawa12.forge:ForgeGradle:1.2-1.0.+") {
            isChanging = true
        }
    }
}

rootProject.name = "anti-raid-weapons"
