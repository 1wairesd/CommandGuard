plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.wairesdindustries.commandguard"
version = "2025.04.2"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.viaversion.com")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.1-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.viaversion:viaversion-api:4.3.1")
    compileOnly("io.github.waterfallmc:waterfall-api:1.18-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation("me.carleslc.Simple-YAML:Simple-Yaml:1.8")
    implementation("org.yaml:snakeyaml:1.30")
}

val relocateLibs: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.() -> Unit = {
    relocate("org.simpleyaml", "com.wairesdindustries.commandguard.libs.simpleyaml")
    relocate("org.yaml.snakeyaml", "com.wairesdindustries.commandguard.libs.snakeyaml")
}

// ── Spigot/Paper jar (contains plugin.yml, no bungee.yml) ──────────────────
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowSpigot") {
    group = "build"
    archiveClassifier.set("spigot")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("bungee.yml")
    exclude("velocity-config.yml")
    relocateLibs()
}

// ── Velocity jar (no plugin.yml, no bungee.yml — uses @Plugin annotation) ──
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowVelocity") {
    group = "build"
    archiveClassifier.set("velocity")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("plugin.yml")
    exclude("bungee.yml")
    relocateLibs()
}

// ── Waterfall/BungeeCord jar (contains bungee.yml, no plugin.yml) ──────────
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowWaterfall") {
    group = "build"
    archiveClassifier.set("waterfall")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("plugin.yml")
    exclude("velocity-config.yml")
    relocateLibs()
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-config.yml", "bungee-config.yml", "config.yml")) {
            expand("version" to project.version)
        }
    }

    // Disable default shadowJar to avoid confusion
    named("shadowJar") {
        enabled = false
    }

    build {
        dependsOn("shadowSpigot", "shadowVelocity", "shadowWaterfall")
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}
