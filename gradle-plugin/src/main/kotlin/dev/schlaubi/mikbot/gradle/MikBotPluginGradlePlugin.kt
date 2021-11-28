package dev.schlaubi.mikbot.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import java.nio.file.Path
import kotlin.io.path.name

// There might be a better way of doing this, but for now I can't be bothered figuring it out
private val Project.pluginMainFile: Path
    get() = buildDir
        .resolve("generated")
        .resolve("ksp")
        .resolve("main")
        .resolve("resources")
        .resolve("META-INF")
        .resolve("MANIFEST.MF")
        .toPath()

@Suppress("unused")
class MikBotPluginGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            createPluginExtensions()
            configureTasks()
        }
    }

    private fun Project.configureTasks() {
        val patchPropertiesTask = createPatchPropertiesTask()

        val jar = tasks.findByName("jar") as Jar? ?: pluginNotAppliedError("Kotlin")
        jar.dependsOn(patchPropertiesTask)
        val assemblePlugin = createAssemblePluginTask(jar)
        createPublishingTasks(assemblePlugin)
    }

    private fun Project.createPatchPropertiesTask() =
        tasks.run {
            task<PatchPropertiesTask>("patchPluginProperties") {
                group = "mikbot-plugin"

                dependsOn("kspKotlin")
                propertiesFile.set(mikbotPluginExtension.pluginMainFileLocation.getOrElse(project.pluginMainFile))
            }
        }

    // Taken from: https://github.com/twatzl/pf4j-kotlin-demo/blob/master/plugins/build.gradle.kts#L20-L35
    // Funfact: because the kotlin dsl is missing we only have groovy api
    // this means all the lambdas are normal lambdas and not lambdas with receivers
    // therefore not calling it, would always use the main spec
    // which took me 4 hrs to figure out
    private fun Project.createAssemblePluginTask(jarTask: Jar) =
        tasks.run {
            register<Jar>("assemblePlugin") {
                group = "build"
                dependsOn(jarTask)

                duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                destinationDirectory.set(buildDir.resolve("plugin"))
                archiveBaseName.set("plugin-${project.name}")
                archiveExtension.set("zip")

                // first taking the classes generated by the jar task
                into("classes") {
                    it.with(jarTask)
                }

                // and then we also need to include any libraries that are needed by the plugin
                dependsOn(configurations.getByName("runtimeClasspath"))
                into("lib") {
                    it.from({
                        val mainConfiguration = if (!mikbotPluginExtension
                                .ignoreDependencies
                                .getOrElse(false)
                        ) {
                            transientDependencies
                                .lines()
                                .filterNot { file -> file.startsWith("#") || file.isBlank() }
                        } else {
                            emptyList()
                        }

                        // filter out dupe dependencies
                        configurations.getByName("runtimeClasspath").files.filter { file ->
                            file.removeVersion() !in mainConfiguration
                        }
                    })
                }

                into("") { // not specifying "" brakes Gradle btw
                    val file = mikbotPluginExtension.pluginMainFileLocation
                        .getOrElse(pluginMainFile)
                    it.from(file.parent)
                    it.include(file.name)
                }
        }
    }

    private fun Project.createPluginExtensions() {
        extensions.create<PluginExtension>(pluginExtensionName)

        val pluginConfiguration = configurations.create("plugin")
        val optionalPluginConfiguration = configurations.create("optionalPlugin")

        val compileOnly = configurations.findByName("compileOnly")
            ?: pluginNotAppliedError("Kotlin")

        compileOnly.apply {
            extendsFrom(pluginConfiguration)
            extendsFrom(optionalPluginConfiguration)
        }
    }

    private fun Project.createPublishingTasks(assemblePluginTask: TaskProvider<Jar>) {
        tasks.apply {
            val buildRepository = register<MakeRepositoryIndexTask>("buildRepository") {
                group = "publishing"
            }

            register<Copy>("copyFilesIntoRepo") {
                group = "publishing"
                dependsOn(buildRepository)

                from(assemblePluginTask)
                include("*.zip")
                // providing version manually, as of weird evaluation errors
                into(buildRepository.get().targetDirectory.get().resolve("${project.name}/$version"))
            }
        }
    }
}

private inline fun <reified T> ExtensionContainer.create(name: String) = create(name, T::class.java)
private inline fun <reified T : Task> TaskContainer.task(name: String, crossinline block: T.() -> Unit) =
    create(name, T::class.java) {
        it.block()
    }

private inline fun <reified T : Task> TaskContainer.register(name: String, crossinline block: T.() -> Unit) =
    register(name, T::class.java) {
        it.block()
    }

private fun pluginNotAppliedError(name: String): Nothing =
    error("Please make sure the $name plugin is applied before the mikbot plugin")
