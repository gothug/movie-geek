package mvgk.mailer

import java.sql.Timestamp

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import mvgk.db.DB
import mvgk.db.MyPostgresDriver.simple._
import mvgk.db.model.ResourceEnum._
import mvgk.db.model.Tables._
import mvgk.httpservice.JsonSupport._
import mvgk.moviesearch.{KickassQuery, MovieQueryResult}
import mvgk.user
import mvgk.watchlistparser.{WatchListMovies, WatchListParsedMovie, WatchListQuery}
import mvgk.util.retry

/**
 * @author Got Hug
 */
class Mailer(implicit val actorSystem: ActorSystem, implicit val timeout: Timeout) {
  import actorSystem.dispatcher
  import mvgk.mailer.ProcessItemActor._

  private val logger = Logger(LoggerFactory.getLogger("mailer"))

  private val processItemActor = actorSystem.actorOf(
    Props(new ProcessItemActor()).withRouter(RoundRobinRouter(8)), "processitem")

  def processDb(foundMovies: List[(WatchListParsedMovie, MovieQueryResult)], resourceName: ResourceEnum) = {
    def findOrInsertIntoFilm(title: String, year: Int): FilmRow = {
      DB.db withSession {
        implicit session =>
          def getFilm = {
            film.filter(x => x.title === title && x.year === year).firstOption
          }

          val item = getFilm

          item match {
            case Some(x) => x
            case None =>
              try {
                val filmId = (film returning film.map(_.id)) += FilmRow(Int.MinValue, title, year)
                getFilm.get
              } catch {
                case e: Exception =>
                  getFilm.get
              }
          }
      }
    }

    def updateOrInsertIntoSearch(filmId: Int, resourceId: Int, hash: String) = {
      DB.db withSession {
        implicit session =>
          val TmStamp = new Timestamp(System.currentTimeMillis())

          val updateQuery = for {
            s <- search if s.filmId === filmId && s.resourceId === resourceId
          } yield (s.hash, s.updateTime)

          def update(): Int = {
            updateQuery.update(hash, TmStamp)
          }

          val updateCount = update()

          if (updateCount == 0) {
            try {
              search += SearchRow(filmId, resourceId, hash, TmStamp)
            } catch {
              case e: Exception => update()
            }
          }
      }
    }

    def getResourceId(resourceName: ResourceEnum): Int = {
      DB.db withSession {
        implicit session =>
          resource.filter(_.resource === resourceName).firstOption.get.id
      }
    }

    def getLastSearch(filmId: Int, resourceId: Int): Option[SearchRow] = {
      DB.db withSession {
        implicit session =>
          search.filter(x => x.filmId === filmId && x.resourceId === resourceId).firstOption
      }
    }

    val resourceId = getResourceId(resourceName)

    val foundMoviesFiltered: ListBuffer[(WatchListParsedMovie, MovieQueryResult)] = ListBuffer()

    for ((msearch, mfound) <- foundMovies) {
      val title = msearch.title
      val year  = msearch.year
      val md5   = mfound.md5

      val filmId = findOrInsertIntoFilm(title, year).id

      val lastSearch: Option[SearchRow] = getLastSearch(filmId, resourceId)

      updateOrInsertIntoSearch(filmId, resourceId, md5)

      if (lastSearch.map { _.hash == md5 } != Some(true)) {
        val m: (WatchListParsedMovie, MovieQueryResult) = (msearch, mfound)
        foundMoviesFiltered += m
      }
    }

    foundMoviesFiltered
  }

  def processWatchLists() = {
    def getMovieTitles: WatchListMovies = {
      val pipeline: HttpRequest => Future[WatchListMovies] = sendReceive ~> unmarshal[WatchListMovies]
      val url = "http://localhost:8080/watchlist/imdb"
      val watchlistLink = "http://www.imdb.com/user/ur9112878/watchlist?ref_=wt_nv_wl_all_0"
      val response = pipeline(Post(url, WatchListQuery(watchlistLink)))
      Await.result(response, 25 seconds)
    }

    def toHtml(title: String, year: Int, link: Option[String]) = {
      val aTag = link match {
        case Some(x) => s"""<a href="$x">Link</a>"""
        case None => """<font style="color:red">Not Found</font>"""
      }

      s"""<p>Title: $title, Year: $year<br>$aTag</p>""".stripMargin
    }

    logger.info("Mailer - process watchlists called..")

    val movieTitles = retry(30000, 1000) { getMovieTitles } //retry every second, max 30 secs

    val url = "http://localhost:8080/search/kickass"

    val moviesList = movieTitles.list //.filter(x => ("london".r findFirstIn x.title).isDefined)

    val responses: List[Future[MovieQueryResult]] =
      for {
        m <- moviesList
      } yield (processItemActor ? Item(url, m))(500 seconds).mapTo[MovieQueryResult]

    val results: List[MovieQueryResult] = Await.result(Future.sequence(responses), 6500 seconds)

    val foundMovies: List[(WatchListParsedMovie, MovieQueryResult)] = moviesList zip results

    logger.info("Finished processing..")

    val htmls =
      for {
        ( WatchListParsedMovie(title, year),
          MovieQueryResult(link, md5) ) <- processDb(foundMovies, Kickass)
      } yield toHtml(title, year, link)

    sendMail(htmls.mkString)
  }

  def sendMail(html: String) = {
    val proto = "https://"
    val domain = "mandrillapp.com"
    val basepath = "/api/1.0"
    val path = "/messages/send.json"

    val url = proto + domain + basepath + path

    val htmlEscaped = html.replace(""""""", """\"""")

    val emailAddressToJson =
      """|       {
         |          "email" : "%s",
         |          "name" : "%s",
         |          "type" : "%s"
         |       }""".stripMargin

    val emailAddresses = user.User.user.accounts

    val emailAddressesJson =
      emailAddresses.filter(_.subscribed == true).
        map(x => emailAddressToJson.format(x.email, x.name.getOrElse("Unknown Name"), "to")).mkString(",\n")

    val emailJson =
      s"""|{
          |   "key" : "BAXsOmtwxAELZKVQWohlzQ",
          |   "message" : {
          |     "html" : "$htmlEscaped",
          |     "text" : "Lack of news.",
          |     "subject" : "Here are you titles, geek",
          |     "from_email" : "geek@movie.com",
          |     "from_name" : "Mr. IMDB geek",
          |     "to" : [
          |$emailAddressesJson
          |     ],
          |     "headers" : {
          |       "Reply-To" : "kojuhovskiy@gmail.com"
          |     },
          |     "auto_html": true
          |   }
          |}""".stripMargin


    val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
    val response: Future[HttpResponse] = pipeline(Post(url, emailJson))

    logger.info("Sending e-mail..")

    val r = Await.result(response, 65 seconds)

    if (r.status.toString == "200 OK") {
      logger.info("Email sent successfully")
    } else {
      logger.info("Problems with sending email")
      logger.info(r.entity.toString)
    }
  }
}

object ProcessItemActor {
  case class Item(url: String, query: WatchListParsedMovie)
}

class ProcessItemActor() extends Actor {
  import context.dispatcher
  import mvgk.mailer.ProcessItemActor._

  override def receive = {
    case Item(url, wlMovie) => sender ! processRequest(url, wlMovie)
  }

  def processRequest(url: String, movie: WatchListParsedMovie): MovieQueryResult = {
    val pipeline: HttpRequest => Future[MovieQueryResult] = sendReceive ~> unmarshal[MovieQueryResult]
    val response: Future[MovieQueryResult] = pipeline(Post(url, KickassQuery(movie.title, None, movie.year)))
    Await.result(response, 30 seconds)
  }
}

object Mailer extends App {
  implicit val actorSystem = ActorSystem()
  implicit val timeout = Timeout(30.second)

  val mailer = new Mailer()
  mailer.processWatchLists()

  actorSystem.shutdown()
}
