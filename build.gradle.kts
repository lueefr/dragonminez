plugins {
    java
    id("net.fabricmc.fabric-loom") version "1.15.5"
}
version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("mod_group_id").get()
base { archivesName.set("dragonminez-26.1.2-fabric") }
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}
repositories {
    mavenCentral()
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
}
dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.20.1")
    implementation("net.fabricmc.fabric-api:fabric-api:0.155.3+26.1.2")
    implementation("com.geckolib:geckolib-fabric-26.1.2:5.5.2")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    compileOnly("com.zaxxer:HikariCP:7.1.0")
}
sourceSets.main { resources.srcDir("src/generated/resources") }
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

// These verify the independent extension algorithms while the full platform port is in progress.
// They do not replace compileJava, the mod build, or gameplay verification.
val extensionTestOutput = layout.buildDirectory.dir("extension-api-tests")
val compileExtensionApiCoreTests = tasks.register<JavaCompile>("compileExtensionApiCoreTests") {
    source("src/main/java/com/dragonminez/api/forms/FormCompositionResolver.java",
           "src/main/java/com/dragonminez/api/content/ContentProgress.java",
           "src/main/java/com/dragonminez/api/lang/ConfigTranslations.java",
           "src/main/java/com/dragonminez/api/npc/CustomNpcRegistry.java")
    source(fileTree("porting/tests") { include("*.java") })
    classpath = configurations.compileClasspath.get()
    destinationDirectory.set(extensionTestOutput)
}
val extensionApiCoreTests = mapOf(
    "verifyFormComposition" to "FormCompositionTest",
    "verifyCustomConfigs" to "CustomConfigTest",
    "verifyContentProgress" to "com.dragonminez.api.content.ContentProgressTest"
).map { (taskName, entrypoint) ->
    tasks.register<JavaExec>(taskName) {
        dependsOn(compileExtensionApiCoreTests)
        classpath = files(extensionTestOutput) + configurations.compileClasspath.get()
        mainClass.set(entrypoint)
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
        if (entrypoint == "CustomConfigTest") {
            val scratch = layout.buildDirectory.dir("tmp/custom-config-tests")
            doFirst { scratch.get().asFile.mkdirs() }
            args(scratch.get().asFile.absolutePath)
        }
    }
}
tasks.register("verifyExtensionApiCore") { dependsOn(extensionApiCoreTests) }
tasks.named("check") { dependsOn("verifyExtensionApiCore") }
