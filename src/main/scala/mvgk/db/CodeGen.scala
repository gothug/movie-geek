package mvgk.db

/**
 * @author Got Hug
 */
object CodeGen extends App {
  val slickDriver = "scala.slick.driver.PostgresDriver"
  val jdbcDriver = "org.postgresql.Driver"
  val url = "jdbc:postgresql://localhost/moviedb"
  val outputFolder = "/var/tmp"
  val pkg = "mvgk.db.model"
  val user = "postgres"
  val password = "q1"

  scala.slick.codegen.SourceCodeGenerator.main(
    Array(slickDriver, jdbcDriver, url, outputFolder, pkg, user, password)
  )
}
