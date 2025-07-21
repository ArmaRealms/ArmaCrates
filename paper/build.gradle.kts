import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.tasks.RemapJar

plugins {
    id("paper-plugin")
}

dependencies {
    api(project(":common"))
    implementation(libs.cluster.paper)
    implementation(libs.triumphcmds)
    implementation(libs.nbtapi)
    compileOnly(libs.decentholograms)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.oraxen)
    compileOnly(fileTree("libs").include("*.jar"))
}

tasks.shadowJar {
    listOf(
        "com.ryderbelserion.cluster.paper",
        "de.tr7zw.changeme.nbtapi",
        "dev.triumphteam.cmd",
        "org.bstats"
    ).forEach { relocate(it, "libs.$it") }
}

tasks.named<RemapJar>("reobfJar") {
    val shadow = tasks.named<ShadowJar>("shadowJar")
    val jarTask = tasks.named<Jar>("jar")

    dependsOn(jarTask, shadow)

    inputJar.set(shadow.flatMap { it.archiveFile })
}

tasks.build { dependsOn(tasks.named("reobfJar")) }

tasks {
    processResources {
        val properties = hashMapOf(
            "name" to rootProject.name,
            "version" to project.version,
            "group" to rootProject.group,
            "description" to rootProject.description,
            "apiVersion" to providers.gradleProperty("apiVersion").get(),
            "authors" to providers.gradleProperty("authors").get(),
            "website" to providers.gradleProperty("website").get()
        )

        inputs.properties(properties)

        filesMatching("plugin.yml") {
            expand(properties)
        }
    }
}