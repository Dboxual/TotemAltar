plugins {
    java
}

group = "com.totemaltars"
version = "1.1.23"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.jar {
    archiveBaseName.set("TotemAltars")
    destinationDirectory.set(file("build"))
    manifest {
        attributes["Main-Class"] = "com.totemaltars.TotemAltars"
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
