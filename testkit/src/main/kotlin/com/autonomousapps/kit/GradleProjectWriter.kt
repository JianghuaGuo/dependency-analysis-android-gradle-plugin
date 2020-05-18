package com.autonomousapps.kit

import java.nio.file.Path

class GradleProjectWriter(
  private val gradleProject: GradleProject
) {

  fun write() {
    val rootDir = gradleProject.rootDir
    rootDir.mkdirs()

    val rootPath = rootDir.toPath()

    // gradle.properties
    val gradleProperties = rootPath.resolve("gradle.properties")
    gradleProperties.toFile().writeText(gradleProject.rootProject.gradleProperties.toString())

    // Settings script
    val settingsFile = rootPath.resolve("settings.gradle")
    settingsFile.toFile().writeText(gradleProject.rootProject.settingsScript.toString())

    // Root build script
    val rootBuildScript = rootPath.resolve("build.gradle")
    rootBuildScript.toFile().writeText(gradleProject.rootProject.buildScript.toString())

    // (Optional) Source
    gradleProject.rootProject.sources.forEach { source ->
      val sourceRootPath = rootPath.resolve("src/main/${source.sourceType.value}")
      val sourcePath = sourceRootPath.resolve(source.path)
      sourcePath.toFile().mkdirs()
      val filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

      filePath.toFile().writeText(source.source)
    }

    // (Optional) Subprojects
    gradleProject.subprojects.forEach { subproject ->
      if (subproject is AndroidSubproject) {
        AndroidSubprojectWriter(rootPath, subproject).write()
      } else {
        SubprojectWriter(rootPath, subproject).write()
      }
    }
  }

  private open class SubprojectWriter(
    rootPath: Path,
    private val subproject: Subproject
  ) {

    protected val projectPath: Path = rootPath.resolve(subproject.name).also {
      it.toFile().mkdirs()
    }

    open fun write() {
      // Build script
      val buildScriptPath = projectPath.resolve("build.gradle")
      buildScriptPath.toFile().writeText(subproject.buildScript.toString())

      // Sources
      subproject.sources.forEach { source ->
        val sourceRootPath = projectPath.resolve("src/main/${source.sourceType.value}")
        val sourcePath = sourceRootPath.resolve(source.path)
        sourcePath.toFile().mkdirs()
        val filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

        filePath.toFile().writeText(source.source)
      }
    }
  }

  private class AndroidSubprojectWriter(
    rootPath: Path,
    private val androidSubproject: AndroidSubproject
  ) : SubprojectWriter(rootPath, androidSubproject) {

    override fun write() {
      super.write()

      val manifestPath = projectPath.resolve("src/main/AndroidManifest.xml")
      manifestPath.parent.toFile().mkdirs()
      manifestPath.toFile().writeText(androidSubproject.manifest.toString())

      val stylesPath = projectPath.resolve("src/main/res/values/styles.xml")
      stylesPath.parent.toFile().mkdirs()
      stylesPath.toFile().writeText(androidSubproject.styles.toString())

      val colorsPath = projectPath.resolve("src/main/res/values/colors.xml")
      colorsPath.parent.toFile().mkdirs()
      colorsPath.toFile().writeText(androidSubproject.colors.toString())

      val layoutsPath = projectPath.resolve("src/main/res/layout/")
      layoutsPath.toFile().mkdirs()
      androidSubproject.layouts.forEach { layout ->
        val layoutPath = layoutsPath.resolve(layout.filename)
        layoutPath.toFile().writeText(layout.content)
      }
    }
  }
}