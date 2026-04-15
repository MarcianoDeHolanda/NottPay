plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "nottabaker.ve"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly(files("Lib/EdTools-API.jar", "Lib/EdLib-API.jar", "Lib/EdDungeons-API.jar"))
    implementation("com.zaxxer:HikariCP:5.1.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("NottPay-${project.version}.jar")
        relocate("com.zaxxer.hikari", "ve.nottabaker.nottpay.libs.hikari")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching(listOf("paper-plugin.yml")) {
            expand(
                "version" to project.version
            )
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}
