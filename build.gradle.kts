import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("maven-publish")
    id("forge")
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// These settings allow you to choose what version of Java you want to be compatible with. Forge 1.7.10 runs on Java 6 to 8.
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

version = project.property("version").toString()
group = project.property("group").toString()

minecraft {
    version = "${project.property("minecraft_version")}-${project.property("forge_version")}"
    runDir = "run"
}

repositories {
    mavenCentral()
}

val generator by sourceSets.creating

val shade by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    shade(kotlin("stdlib-jdk8"))
    generator.implementationConfigurationName("com.squareup:kotlinpoet:1.10.2")
}

tasks.processResources {
    // This will ensure that this task is redone when the versions change.
    inputs.property("version", project.version)
    inputs.property("mcversion", project.minecraft.version)

    // Replace values in only mcmod.info.
    filesMatching("mcmod.info") {
        // Replace version and mcversion.
        expand(
            mapOf(
                "version" to project.version,
                "mcversion" to project.minecraft.version
            )
        )
    }
}

// Ensures that the encoding of source files is set to UTF-8, see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    shade.forEach { dep ->
        from(project.zipTree(dep)) {
            exclude("META-INF", "META-INF/**")
            exclude("COPYING")
            exclude("COPYING.LESSER")
            exclude("NOTICE")
        }
    }
}

val shadowModJar by tasks.creating(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    dependsOn(tasks.reobf.get())

    val basePkg = "xyz.fmdc.arw.libs"
    relocate("kotlin.", "$basePkg.kotlin.")
    relocate("kotlinx.", "$basePkg.kotlinx.")
    relocate("org.intellij.lang.annotations.", "$basePkg.ij_annotations.")
    relocate("org.jetbrains.annotations.", "$basePkg.jb_annotations.")

    from(provider { zipTree(tasks.jar.get().archiveFile) })
    destinationDirectory.set(buildDir.resolve("shadowing"))
    archiveVersion.set("")
    manifest.from(provider {
        zipTree(tasks.jar.get().archiveFile)
            .matching { include("META-INF/MANIFEST.MF") }
            .files.first()
    })
}

val copyShadowedJar by tasks.creating {
    dependsOn(shadowModJar)
    doLast {
        shadowModJar.archiveFile.get().asFile.inputStream().use { src ->
            tasks.jar.get().archiveFile.get().asFile.apply { parentFile.mkdirs() }
                .outputStream()
                .use { dst -> src.copyTo(dst) }
        }
    }
}

tasks.assemble.get().dependsOn(copyShadowedJar)

val generatedSourceDir = "$projectDir/src/main/generated"

java.sourceSets["main"].java {
    srcDir(generatedSourceDir)
}

val generateCode by tasks.creating(JavaExec::class) {
    dependsOn(tasks[generator.classesTaskName])
    inputs.files(tasks[generator.classesTaskName].outputs)
    outputs.files(fileTree(generatedSourceDir).include("**/*.g.kt"))
    mainClass.set("xyz.fmdc.arw.gen.Main")
    classpath = generator.runtimeClasspath
    args(generatedSourceDir)
    tasks.compileKotlin.get().dependsOn(this)
    tasks.sourceMainJava.get().dependsOn(this)
}

// This task creates a .jar file containing the source code of this mod.
val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(tasks.classes.get())
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// This task creates a .jar file containing a deobfuscated version of this mod, for other developers to use in a development environment.
val devJar by tasks.creating(Jar::class) {
    archiveClassifier.set("dev")
    from(sourceSets.main.get().output)
}

// Creates the listed artifacts on building the mod.
artifacts {
    archives(sourcesJar)
    archives(devJar)
}

// This block configures any maven publications you want to make.
publishing {
    publications {
        @Suppress("UNUSED_VARIABLE")
        val mavenJava by creating(MavenPublication::class) {
            // Add any other artifacts here that you would like to publish!
            artifact(tasks.jar) {
                builtBy(tasks.build)
            }
            artifact(sourcesJar) {
                builtBy(sourcesJar)
            }
            artifact(devJar) {
                builtBy(devJar)
            }
        }
    }

    // This block selects the repositories you want to publish to.
    repositories {
        // Add the repositories you want to publish to here.
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
