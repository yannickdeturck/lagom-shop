import ByteConversions._

organization in ThisBuild := "be.yannickdeturck.lagomshop"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.12"

val immutables = "org.immutables" % "value" % "2.1.14"
val mockito = "org.mockito" % "mockito-core" % "1.10.19"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"

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
    libraryDependencies ++= Seq(lagomJavadslPersistenceCassandra, immutables,
      lagomJavadslImmutables, lagomJavadslTestKit, lagomJavadslPubSub, lagomJavadslKafkaBroker, mockito)
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
    libraryDependencies ++= Seq(lagomJavadslPersistenceCassandra, immutables,
      lagomJavadslImmutables, lagomJavadslTestKit, lagomJavadslKafkaBroker
    )
  )
  .settings(lagomForkedTestSettings: _*) // tests must be forked for cassandra
  .dependsOn(itemApi)

lazy val frontEnd = project("front-end")
  .enablePlugins(PlayScala && LagomPlay)
  .settings(
    version := "1.0-SNAPSHOT",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      "org.webjars" % "jquery" % "2.2.3",
      "org.webjars" % "bootstrap" % "3.3.6",
      "com.typesafe.conductr" %% "lagom10-conductr-bundle-lib" % "1.4.7",
      lagomScaladslServer,
      macwire,
      filters,
      "com.adrianhurt" %% "play-bootstrap" % "1.1-P25-B3"
    ),
    // needed to resolve lagom10-conductr-bundle-lib
    resolvers ++= Seq(
      Resolver.bintrayRepo("typesafe", "maven-releases"),
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
    ),
    // ConductR settings
    BundleKeys.nrOfCpus := 1.0,
    BundleKeys.memory := 64.MiB,
    BundleKeys.diskSpace := 35.MB,
    BundleKeys.endpoints := Map("web" -> Endpoint("http", services = Set(URI("http://:9000")))),
    javaOptions in Bundle ++= Seq("-Dhttp.address=$WEB_BIND_IP", "-Dhttp.port=$WEB_BIND_PORT")
  )

def project(id: String) = Project(id, base = file(id))
  .settings(javacOptions in compile ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"))
  .settings(jacksonParameterNamesJavacSettings: _*) // applying it to every project even if not strictly needed.


// See https://github.com/FasterXML/jackson-module-parameter-names
lazy val jacksonParameterNamesJavacSettings = Seq(
  javacOptions in compile += "-parameters"
)

lagomCassandraCleanOnStart in ThisBuild := true
