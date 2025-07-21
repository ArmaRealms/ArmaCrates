import io.papermc.paperweight.tasks.RemapJar
import java.util.Locale

fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

plugins { id("root-plugin") }

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.assemble {
    val jarsDir = rootDir.resolve("jars")

    doFirst { delete(jarsDir); jarsDir.mkdirs() }

    subprojects.filter { it.name in listOf("paper", "fabric") }.forEach { sub ->
        val reobf = sub.tasks.named<RemapJar>("reobfJar")
        dependsOn(reobf)

        doLast {
            val dest = jarsDir.resolve(sub.name.cap())
            dest.mkdirs()

            copy {
                from(reobf.flatMap { it.outputJar })
                into(dest)
            }
        }
    }
}