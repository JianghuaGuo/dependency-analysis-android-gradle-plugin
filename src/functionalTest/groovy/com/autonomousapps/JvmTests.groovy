package com.autonomousapps

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.fixtures.JvmAutoServiceProject
import com.autonomousapps.fixtures.JvmDaggerProject
import com.autonomousapps.fixtures.LibrarySpec
import com.autonomousapps.fixtures.MultiModuleJavaLibraryProject
import com.autonomousapps.fixtures.ProjectDirProvider
import com.autonomousapps.fixtures.RedundantJavaLibraryAndKaptPluginsProject
import com.autonomousapps.fixtures.RedundantJavaLibraryPluginProject
import com.autonomousapps.fixtures.RedundantKotlinJvmPluginProject
import com.autonomousapps.fixtures.RootSpec
import com.autonomousapps.fixtures.SimpleKotlinJvmProject
import com.autonomousapps.fixtures.SingleProject
import com.autonomousapps.advice.Advice
import spock.lang.Unroll

import static com.autonomousapps.fixtures.Fixtures.DEFAULT_PACKAGE_NAME
import static com.autonomousapps.fixtures.JvmFixtures.*
import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail

final class JvmTests extends AbstractFunctionalTest {

  private ProjectDirProvider javaLibraryProject = null

  def cleanup() {
    if (javaLibraryProject != null) {
      clean(javaLibraryProject)
    }
  }

  @Unroll
  def "reports redundant java-library and kapt plugins applied (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantJavaLibraryAndKaptPluginsProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    actualAdvice == RedundantJavaLibraryAndKaptPluginsProject.expectedAdvice().first().pluginAdvice

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "reports redundant java-library plugin applied (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantJavaLibraryPluginProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    actualAdvice == RedundantJavaLibraryPluginProject.expectedAdvice().first().pluginAdvice

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "reports redundant kotlin-jvm plugin applied (#gradleVersion)"() {
    given:
    javaLibraryProject = new RedundantKotlinJvmPluginProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<PluginAdvice> actualAdvice = javaLibraryProject.buildHealthFor(":").first().pluginAdvice
    actualAdvice == RedundantKotlinJvmPluginProject.expectedAdvice().first().pluginAdvice

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "can analyze kotlin-jvm projects (#gradleVersion)"() {
    given:
    javaLibraryProject = new SimpleKotlinJvmProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    actualAdvice == SimpleKotlinJvmProject.expectedAdvice()

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "autoservice is used with annotationProcessor (#gradleVersion)"() {
    given:
    javaLibraryProject = new JvmAutoServiceProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    actualAdvice == JvmAutoServiceProject.expectedAdvice()

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "dagger is unused with annotationProcessor (#gradleVersion)"() {
    given:
    javaLibraryProject = new JvmDaggerProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    actualAdvice == JvmDaggerProject.expectedAdvice()

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "root projects can contain source (#gradleVersion)"() {
    given:
    javaLibraryProject = new SingleProject()

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    Set<Advice> actualAdvice = javaLibraryProject.adviceFor(":")
    actualAdvice == SingleProject.expectedAdvice()

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "configuration fails with sane error message if plugin was not applied to root (#gradleVersion)"() {
    given:
    def libSpecs = [JAVA_ERROR]
    def rootSpec = new RootSpec(
      libSpecs, "", RootSpec.defaultGradleProperties(), null,
      RootSpec.defaultSettingsScript(null, libSpecs),
      noPluginBuildScript(libSpecs)
    )
    javaLibraryProject = new MultiModuleJavaLibraryProject(rootSpec, libSpecs)

    expect:
    def result = buildAndFail(gradleVersion, javaLibraryProject, 'help')
    result.output.contains('You must apply the plugin to the root project. Current project is :error')

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "finds constants in java projects (#gradleVersion)"() {
    given:
    def libSpecs = [CONSUMER_CONSTANT_JAVA, PRODUCER_CONSTANT_JAVA]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_JAVA)
    [] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "finds constants in kotlin projects (#gradleVersion)"() {
    given:
    def libSpecs = [CONSUMER_CONSTANT_KOTLIN, PRODUCER_CONSTANT_KOTLIN]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_KOTLIN)
    [] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "correctly analyzes JVM projects for inline usage (#gradleVersion)"() {
    given:
    def libSpecs = [INLINE_PARENT, INLINE_CHILD]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, 'buildHealth')

    then:
    def actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(INLINE_PARENT)
    ['org.jetbrains.kotlin:kotlin-stdlib-jdk8'] == actualUnusedDependencies

    where:
    gradleVersion << gradleVersions()
  }

  @Unroll
  def "does not declare superclass used when it's only needed for compilation (#gradleVersion)"() {
    given:
    def libSpecs = [ABI_SUPER_LIB, ABI_CHILD_LIB, ABI_CONSUMER_LIB]
    javaLibraryProject = new MultiModuleJavaLibraryProject(RootSpec.defaultRootSpec(libSpecs), libSpecs)

    when:
    build(gradleVersion, javaLibraryProject, "buildHealth")

    then:
    def actualUsedClasses = javaLibraryProject.allUsedClassesFor(ABI_CONSUMER_LIB)
    def expectedUsedClasses = [
      "${DEFAULT_PACKAGE_NAME}.kotlin.ChildClass",
      "${DEFAULT_PACKAGE_NAME}.kotlin.ConsumerClass",
      "kotlin.Metadata"
    ]
    expectedUsedClasses == actualUsedClasses

    and:
    def actualChild = javaLibraryProject.allUsedClassesFor(ABI_CHILD_LIB)
    def expectedChild = [
      "${DEFAULT_PACKAGE_NAME}.kotlin.ChildClass",
      "${DEFAULT_PACKAGE_NAME}.kotlin.SuperClass",
      "kotlin.Metadata"
    ]
    expectedChild == actualChild

    where:
    gradleVersion << gradleVersions()
  }

  private static String noPluginBuildScript(List<LibrarySpec> librarySpecs) {
    """
      buildscript {
        repositories {
          google()
          jcenter()
          maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
        }
        dependencies {
          ${RootSpec.kotlinGradlePlugin(librarySpecs)}
        }
      }
      subprojects {
        repositories {
          google()
          jcenter()
          maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
        }
      }
    """
  }
}
