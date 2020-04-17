package com.autonomousapps

import com.autonomousapps.internal.getPluginJvmAdvicePath
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register

// TODO move this elsewhere
internal class RedundantPluginSubPlugin(
  private val project: Project,
  private val extension: DependencyAnalysisExtension
) {

  fun configure() {
    project.configureRedundantJvmPlugin()
  }

  private fun Project.configureRedundantJvmPlugin() {
    val redundantProjectTask = tasks.register<RedundantPluginAlertTask>("redundantProjectAlert") {
      javaFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.java")
      })
      kotlinFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.kt")
      })
      chatty.set(extension.chatty)
      output.set(layout.buildDirectory.file(getPluginJvmAdvicePath()))
    }

    // Add this as an outgoing artifact
    val advicePluginsReportsConf = configurations.maybeCreate(CONF_ADVICE_PLUGINS_PRODUCER).also {
      it.isCanBeResolved = false
    }
    artifacts {
      add(advicePluginsReportsConf.name, layout.buildDirectory.file(getPluginJvmAdvicePath())) {
        builtBy(redundantProjectTask)
      }
    }
    // Add project dependency on root project to this project, with our new configuration
    rootProject.dependencies {
      add(CONF_ADVICE_PLUGINS_CONSUMER, project(this@configureRedundantJvmPlugin.path, advicePluginsReportsConf.name))
    }
  }
}
