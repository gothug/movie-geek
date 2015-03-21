package mvgk.db

import liquibase.Liquibase
import liquibase.integration.commandline.CommandLineUtils
import liquibase.resource.FileSystemResourceAccessor
import mvgk.config.Config

/**
* @author Got Hug
*/
object DBManager extends App {
  if (args.contains("create")) {
    init(args.contains("drop"))
    updateAll()
  }

  if (args.contains("update")) {
    updateAll()
  }

  def init(drop: Boolean = false) {
    if (drop) {
      DB.safeDrop()
      println("DB dropped") // scalastyle:ignore
    }
    DB.create()
    println("DB created") // scalastyle:ignore
  }

  def updateAll() {
    val uname = Config.db.user
    val pass = Config.db.password
    val driver = Config.db.driver
    val url = "jdbc:postgresql:" + Config.db.name
    val loader = this.getClass.getClassLoader

    val dbChangeLogPath = getClass.getResource("/migrations/db-changelog.xml")

    update(dbChangeLogPath.getFile)
    println("DB updated") // scalastyle:ignore

    def update(file: String): Unit = {
      val database = CommandLineUtils.createDatabaseObject(loader, url, uname, pass, driver, null,
        null, null) // scalastyle:ignore
      val liquibase = new Liquibase(file, new FileSystemResourceAccessor, database)
      liquibase.update("")
    }
  }
}
