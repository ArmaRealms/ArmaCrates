plugins {
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    id("root-plugin")
}

base {
    archivesName.set(rootProject.name)
}

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.triumphteam.dev/snapshots/")
    maven("https://repo.oraxen.com/releases/")
    flatDir { dirs("libs") }
}

val mcVersion = providers.gradleProperty("mcVersion").get()

project.version =
    if (System.getenv("BUILD_NUMBER") != null) "${rootProject.version}-${System.getenv("BUILD_NUMBER")}" else rootProject.version

tasks {
    jar {
        archiveClassifier.set("plain")
    }

    shadowJar {
        archiveClassifier.set("")
        exclude("META-INF/**")
    }

    runServer {
        jvmArgs("-Dnet.kyori.ansi.colorLevel=truecolor")

        defaultCharacterEncoding = Charsets.UTF_8.name()

        minecraftVersion(mcVersion)
    }
}
