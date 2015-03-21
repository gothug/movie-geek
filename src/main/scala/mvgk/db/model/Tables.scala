package mvgk.db.model

import mvgk.db.MyPostgresDriver.simple._
import mvgk.db.model.ResourceEnum._

object Tables {
  val film = TableQuery(new Film(_))
  val resource = TableQuery(new Resource(_))
  val search = TableQuery(new Search(_))

  /** Film **/
  case class FilmRow(id: Int, title: String, year: Int)

  class Film(_tableTag: Tag) extends Table[FilmRow](_tableTag, "film") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def title = column[String]("title", O.Length(2147483647, varying = true))

    def year = column[Int]("year")

    def * = (
      id,
      title,
      year) <> (FilmRow.tupled, FilmRow.unapply)
  }

  /** Resource **/
  case class ResourceRow(id: Int, resource: ResourceEnum)

  class Resource(_tableTag: Tag) extends Table[ResourceRow](_tableTag, "resource") {
    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def resource = column[ResourceEnum]("resource", O.Length(2147483647, varying = true))

    def * = (
      id,
      resource) <> (ResourceRow.tupled, ResourceRow.unapply)
  }

  /** Search **/
  case class SearchRow(filmId: Int, resourceId: Int, hash: String, updateType: java.sql.Timestamp)

  class Search(_tableTag: Tag) extends Table[SearchRow](_tableTag, "search") {
    def filmId = column[Int]("film_id")

    def resourceId = column[Int]("resource_id")

    def hash = column[String]("hash", O.Length(2147483647, varying = true))

    def updateTime = column[java.sql.Timestamp]("update_time")

    val pk = primaryKey("pk_search", (filmId, resourceId))

    lazy val filmFk = foreignKey("search_film_id_fk", filmId, film)(r =>
      r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    lazy val resourceFk = foreignKey("search_resource_id_fk", resourceId, resource)(r =>
      r.id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)

    def * = (
      filmId,
      resourceId,
      hash,
      updateTime) <> (SearchRow.tupled, SearchRow.unapply)
  }
}

