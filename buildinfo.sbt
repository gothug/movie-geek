buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "sbtbuildinfo"
