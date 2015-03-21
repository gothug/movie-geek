import sbtrelease._
import ReleaseStateTransformations._

lazy val runDeploy =
  ReleaseStep({ state =>
    if ("./deploy.sh".! != 0) {
      sys.error("Error in deploy!")
    }
    state
  })

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  runDeploy
)
