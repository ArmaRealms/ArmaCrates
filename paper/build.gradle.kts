plugins {
    id("paper-plugin")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    api(project(":common"))
    testImplementation("io.papermc.paper:paper-api:26.2.build.+")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("org.mockito:mockito-core:5.24.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
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

tasks.test {
    useJUnitPlatform()
}
