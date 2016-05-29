organization in ThisBuild := "be.yannickdeturck.lagomshop"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.7"

val immutables = "org.immutables" % "value" % "2.1.14"
val mockito = "org.mockito" % "mockito-core" % "1.10.19"

lazy val orderApi = project("order-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(lagomJavadslApi, immutables,
      lagomJavadslImmutables, lagomJavadslJackson)
  )

lazy val orderImpl = project("order-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(lagomJavadslPersistence, immutables,
      lagomJavadslImmutables, lagomJavadslTestKit, mockito)
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(orderApi, itemApi)

lazy val itemApi = project("item-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(lagomJavadslApi, immutables,
      lagomJavadslImmutables, lagomJavadslJackson)
  )

lazy val itemImpl = project("item-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(lagomJavadslPersistence, immutables,
      lagomJavadslImmutables, lagomJavadslTestKit
    )
  )
  .settings(lagomForkedTestSettings: _*) // tests must be forked for cassandra
  .dependsOn(itemApi)

def project(id: String) = Project(id, base = file(id))
  .settings(javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"))
  .settings(jacksonParameterNamesJavacSettings: _*) // applying it to every project even if not strictly needed.


// See https://github.com/FasterXML/jackson-module-parameter-names
lazy val jacksonParameterNamesJavacSettings = Seq(
  javacOptions in compile += "-parameters"
)

lagomCassandraCleanOnStart in ThisBuild := true
