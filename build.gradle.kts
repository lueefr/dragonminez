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
    maven("https://api.modrinth.com/maven")
}
dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.20.1")
    implementation("net.fabricmc.fabric-api:fabric-api:0.155.3+26.1.2")
    implementation("com.geckolib:geckolib-fabric-26.1.2:5.5.2")
    implementation("maven.modrinth:terrablender:NIXt6lha")
    implementation("teamreborn:energy:5.0.0")
    include("teamreborn:energy:5.0.0")
    compileOnly("maven.modrinth:jei:b5BXTIh2")
    compileOnly("maven.modrinth:modmenu:ZJ6YTrMY")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
    compileOnly("com.zaxxer:HikariCP:7.1.0")
}
sourceSets.main { resources.srcDir("src/generated/resources") }
loom {
    runs {
        register("gameTest") {
            server()
            name("DragonMineZ integration tests")
            runDir("run-gametest")
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=../build/test-results/gametest.xml")
        }
    }
}
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
    source(fileTree("src/main/java/com/dragonminez/platform/registry") { include("*.java") })
    source(fileTree("src/main/java/com/dragonminez/platform/nbt") { include("*.java") })
    source(fileTree("src/main/java/com/dragonminez/compat/network") { include("**/*.java") })
    source(fileTree("src/main/java/com/dragonminez/platform/network") { include("*.java"); exclude("ClientFluidRendering.java") })
    source(fileTree("src/main/java/com/dragonminez/platform/inventory") { include("*.java") })
    source(fileTree("src/main/java/com/dragonminez/server/energy") { include("*.java") })
    source(fileTree("src/main/java/com/dragonminez/platform/resource") { include("*.java") })
    source(fileTree("src/main/java/com/dragonminez/platform/client") { include("model/*.java", "render/*.java") })
    source(fileTree("src/main/java/com/dragonminez/mixin/client") { include("ViewportCameraBridgeMixin.java", "ViewportFogBridgeMixin.java", "ClientRespawnBridgeMixin.java", "KeyboardInputBridgeMixin.java", "MouseInputBridgeMixin.java", "InteractionInputBridgeMixin.java") })
    source(fileTree("src/main/java/com/dragonminez/mixin/common") { include("ProjectileImpactBridgeMixin.java", "ExplosionBridgeMixin.java", "EntityJoinBridgeMixin.java", "EntitySizeBridgeMixin.java", "LivingItemUseBridgeMixin.java", "LivingEffectsBridgeMixin.java", "MobTargetBridgeMixin.java", "LivingIncomingDamageBridgeMixin.java", "LivingAppliedDamageBridgeMixin.java", "PlayerAppliedDamageBridgeMixin.java", "LivingActionsBridgeMixin.java") })
    source("src/main/java/com/dragonminez/platform/entity/EntityData.java",
           "src/main/java/com/dragonminez/platform/entity/SpawnPlacementRegistry.java",
           "src/main/java/com/dragonminez/mixin/common/SpawnPlacementsInvoker.java",
           "src/main/java/com/dragonminez/platform/rules/RuleValues.java")
    source("src/main/java/com/dragonminez/platform/ServerLifecycleHooks.java",
           "src/main/java/com/dragonminez/common/init/fluid/SimpleFluid.java")
    source("src/main/java/com/dragonminez/client/flight/RollCamera.java",
           "src/main/java/com/dragonminez/platform/item/DeferredSpawnEggItem.java",
           "src/main/java/com/dragonminez/common/init/item/tools/ToolTiers.java",
           "src/main/java/com/dragonminez/common/init/item/weapons/BlankWeaponTier.java",
           "src/main/java/com/dragonminez/common/init/item/weapons/WeaponItem.java",
           "src/main/java/com/dragonminez/Reference.java",
           "src/main/java/com/dragonminez/server/commands/DMZPermissions.java",
           "src/main/java/com/dragonminez/common/init/armor/ModArmorMaterials.java",
           "src/main/java/com/dragonminez/common/init/armor/NativeArmorItem.java",
           "src/main/java/com/dragonminez/platform/item/ArmorData.java")
    source(fileTree("src/main/java/com/dragonminez/platform/event") { include("*.java", "entity/*.java", "tick/*.java", "player/*.java", "server/*.java", "level/*.java", "living/*.java", "client/*.java"); exclude("client/InputSubscribers.java", "CommonCustomEventSubscribers.java", "ClientCustomEventSubscribers.java", "tick/CommonTickSubscribers.java", "tick/ClientTickSubscribers.java", "player/PlayerLifecycleSubscribers.java", "player/InteractionSubscribers.java", "server/ServerLifecycleSubscribers.java", "living/LivingSubscribers.java", "client/HudSubscribers.java") })
    classpath = configurations.compileClasspath.get()
    destinationDirectory.set(extensionTestOutput)
}
val extensionApiCoreTests = mapOf(
    "verifyFormComposition" to "FormCompositionTest",
    "verifyCustomConfigs" to "CustomConfigTest",
    "verifyContentProgress" to "com.dragonminez.api.content.ContentProgressTest",
    "verifyNbtMigration" to "NbtMigrationTest",
    "verifyRegistryMigration" to "RegistryMigrationTest",
    "verifyEventDispatch" to "EventDispatchTest",
    "verifyArmorMigration" to "ArmorMigrationTest",
    "verifyNetworkMigration" to "NetworkMigrationTest",
    "verifyRenderExtraction" to "RenderExtractionTest",
    "verifyEntitySerialization" to "EntitySerializationTest",
    "verifyNativeDamageLayout" to "NativeDamageLayoutTest",
    "verifyNativeGameplayLayout" to "NativeGameplayLayoutTest",
    "verifyPackMetadata" to "PackMetadataTest",
    "verifyInventoryMigration" to "InventoryMigrationTest",
    "verifyEnergyMigration" to "EnergyMigrationTest"
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
