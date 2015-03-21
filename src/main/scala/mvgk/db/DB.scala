package mvgk.db

import java.util.Properties
import scala.slick.jdbc.StaticQuery
import scala.util.Try
import mvgk.config.Config
import mvgk.db.MyPostgresDriver.simple._
import Database.dynamicSession
import mvgk.db.model.Tables._
import mvgk.util._

/**
 * @author Got Hug
 */
object DB {
  val driver = "org.postgresql.Driver"
  val name = Config.db.name
  val user = Config.db.user
  val password = Config.db.password
  val url = "jdbc:postgresql"

  val host = getEnvVar("PGSQL_PORT_5432_TCP_ADDR", "localhost")
  val port = getEnvVar("PGSQL_PORT_5432_TCP_PORT", "5432")

  val tables = List(film, resource, search)
  val db = Database.forURL(s"$url://$host:$port/$name", user, password, new Properties(), driver)
  val purePostgres = Database.forURL(s"$url:?port=$port&user=$user&password=$password", driver = driver)

  def create(): Unit = {
    purePostgres.withDynSession {
      StaticQuery.updateNA(s"create database $name").execute
    }
// TODO: not needed currently
//    createEnums()
  }

  def safeDrop(): Unit = {
    purePostgres.withDynSession {
      StaticQuery.updateNA(s"drop database if exists $name").execute
    }
  }

  //todo: make it safe?
  def createTables(): Unit = {
    db.withDynSession {
      tables.reverse.map { table => Try(table.ddl.create)}
    }
  }

// TODO: not needed currently
//  def createEnums(): Unit = {
//    mov.db.withDynSession {
//      buildCreateSql("Status", Statuses).execute
//      buildCreateSql("Regime", Regimes).execute
//      buildCreateSql("Product", Products).execute
//      buildCreateSql("Platform", Platforms).execute
//    }
//  }

  def dropTables(): Unit = {
    db.withDynSession {
      tables.map { table => Try(table.ddl.drop)}
//      dropEnums()
    }
  }

// TODO: not needed currently
//  def dropEnums(): Unit = {
//    buildDropSql("platform").execute
//    buildDropSql("product").execute
//    buildDropSql("regime").execute
//    buildDropSql("status").execute
//  }
}
