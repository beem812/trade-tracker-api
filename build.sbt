val Http4sVersion = "0.21.16"
val CirceVersion = "0.13.0"
val MunitVersion = "0.7.20"
val LogbackVersion = "1.2.3"
val MunitCatsEffectVersion = "0.13.0"
val DoobieVersion = "0.10.0"

lazy val root = (project in file("."))
  .settings(
    organization := "beem812",
    name := "trade-tracker",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.4",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server"      % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client"      % Http4sVersion,
      "org.http4s"      %% "http4s-circe"             % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"               % Http4sVersion,
      "org.http4s" %% "http4s-jdk-http-client" % "0.3.5",
      "io.circe"        %% "circe-generic"            % CirceVersion,
      "org.scalameta"   %% "munit"                    % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect-2"      % MunitCatsEffectVersion % Test,
      "org.tpolecat"          %% "doobie-core"        % DoobieVersion,
      "org.tpolecat"          %% "doobie-hikari"      % DoobieVersion,          // HikariCP transactor.
      "org.tpolecat"          %% "doobie-postgres"    % DoobieVersion,          // Postgres driver 42.2.12 + type mappings.
      "org.tpolecat"          %% "doobie-h2"          % DoobieVersion,
      "org.tpolecat"          %% "doobie-quill"       % "0.10.0",
      "org.mariadb.jdbc"      % "mariadb-java-client" % "2.7.0",
      "ch.qos.logback"        %  "logback-classic"    % LogbackVersion,
      "org.typelevel"         %% "cats-core"          % "2.1.1",
      "org.typelevel"         %% "cats-effect"        % "2.3.1",
      "org.typelevel"         %% "cats-effect-laws" % "2.3.1" % "test",
      "com.github.pureconfig" %% "pureconfig"         % "0.14.0",
      "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.14.0",
      "com.h2database"        % "h2"                  % "1.4.192",
      "org.flywaydb"          %  "flyway-core"        % "7.5.2",
      "io.scalaland" %% "chimney" % "0.6.1",
      "io.scalaland" %% "chimney-cats" % "0.6.1",
      "io.circe" %% "circe-parser" % CirceVersion,
      "com.github.jwt-scala" %% "jwt-circe" % "7.1.3",
      "com.auth0" % "jwks-rsa" % "0.17.1"
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework"),
    Test / parallelExecution := false
  )
