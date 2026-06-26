plugins {
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_17
}

repositories {
    maven {
        name = "Carpet"
        url = uri("https://masa.dy.fi/maven")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    val carpet = "carpet:fabric-carpet:${property("deps.carpet_key")}"
    modCompileOnly(carpet)
    modImplementation(carpet)
}

loom {
    splitEnvironmentSourceSets()

    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "amethystfarm-refmap.json"
    }

    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    mods {
        create("amethystfarm") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(requiredJava.majorVersion.toInt())
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, propertyKey: String) {
            val value: String = sc.properties[propertyKey]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }
        filesMatching("fabric.mod.json") { expand(props) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds this version's jar into build/libs/{mod version}/{mc version}/"
        dependsOn("build")
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        val modVersion = project.property("mod.version").toString()
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion/${sc.current.version}"))
    }
}
