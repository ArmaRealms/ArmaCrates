plugins {
    id("com.github.johnrengelman.shadow")

    id("com.modrinth.minotaur")

    `java-library`

    `maven-publish`
}

base {
    archivesName.set(rootProject.name)
}

repositories {
    maven("https://repo.crazycrew.us/releases")

    maven("https://jitpack.io/")

    mavenCentral()
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveClassifier.set("")

        exclude("META-INF/**")
    }

    val directory = File("$rootDir/jars")
    val mcVersion = rootProject.properties["minecraftVersion"] as String

    modrinth {
        autoAddDependsOn.set(false)

        token.set(System.getenv("modrinth_token"))

        projectId.set("crazyrunes")

        versionName.set("${rootProject.name} ${project.version}")

        versionNumber.set("${project.version}")

        uploadFile.set("$directory/${rootProject.name}-${project.version}.jar")

        gameVersions.add(mcVersion)

        changelog.set(rootProject.file("CHANGELOG.md").readText())
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of("17"))
}