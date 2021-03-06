@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceAggregateReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Aggregates advice reports across all subprojects"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var adviceAllReports: Configuration

  @get:Input
  abstract val chatty: Property<Boolean>

  @get:OutputFile
  abstract val projectReport: RegularFileProperty

  @get:OutputFile
  abstract val projectReportPretty: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

  @TaskAction
  fun action() {
    val projectReportFile = projectReport.getAndDelete()
    val projectReportPrettyFile = projectReportPretty.getAndDelete()

    val comprehensiveAdvice: Map<String, Set<ComprehensiveAdvice>> =
      adviceAllReports.dependencies.map { dependency ->
        val path = (dependency as ProjectDependency).dependencyProject.path

        val compAdvice: Set<ComprehensiveAdvice> = adviceAllReports.fileCollection(dependency)
          .filter { it.exists() }
          .mapToSet { it.readText().fromJson() }

        path to compAdvice.toMutableSet()
      }.mergedMap()

    val buildHealths = comprehensiveAdvice.map { (path, advice) ->
      BuildHealth(
        projectPath = path,
        dependencyAdvice = advice.flatMapToSet { it.dependencyAdvice },
        pluginAdvice = advice.flatMapToSet { it.pluginAdvice }
      )
    }

    projectReportFile.writeText(buildHealths.toJson())
    projectReportPrettyFile.writeText(buildHealths.toPrettyString())

    if (buildHealths.any { it.isNotEmpty() }) {
      chatter.chat("Advice report (aggregated) : ${projectReportFile.path}")
      chatter.chat("(pretty-printed)           : ${projectReportPrettyFile.path}")
    }
  }
}
