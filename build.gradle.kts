import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Locale

fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

plugins { id("root-plugin") }

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.clean {
    delete(layout.projectDirectory.dir("jars"))
}

tasks.assemble {
    val jarsDir = rootDir.resolve("jars")

    doFirst { delete(jarsDir); jarsDir.mkdirs() }

    subprojects.filter { it.name in listOf("paper", "fabric") }.forEach { sub ->
        val shadow = sub.tasks.named<ShadowJar>("shadowJar")
        dependsOn(shadow)

        doLast {
            val dest = jarsDir.resolve(sub.name.cap())
            dest.mkdirs()

            copy {
                from(shadow.flatMap { it.archiveFile })
                into(dest)
            }
        }
    }
}
