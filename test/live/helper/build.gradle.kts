// Test only, for the live tests. Never shipped.
plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.bluecolored.de/releases")
}

dependencies {
    compileOnly(rootProject.extra["paperApi"] as String)
    compileOnly(rootProject.extra["blueMapApi"] as String)
    compileOnly(rootProject) // For CoordinateShareEvent.
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
}

tasks.jar {
    archiveBaseName.set("LiveTestHelper")
}
