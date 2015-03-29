package mvgk.httpservice

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory
import spray.http.{HttpRequest, MediaTypes}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing._
import scala.concurrent.Future
import scala.concurrent.duration._
import mvgk.moviesearch.QueryActor._
import mvgk.moviesearch._
import mvgk.watchlistparser._
import mvgk.mailer._
import mvgk.user

import scala.util.Success

case class Person(name: String, firstName: String, age: Int)

object JsonSupport extends DefaultJsonProtocol {
  implicit val KickassQueryFormat = jsonFormat3(KickassQuery.apply)
  implicit val RutrackerQueryFormat = jsonFormat3(RutrackerQuery.apply)
  implicit val AfishaQueryFormat = jsonFormat3(AfishaQuery.apply)
  implicit val ResultFormat = jsonFormat2(MovieQueryResult.apply)

  implicit val WatchlistQueryFormat = jsonFormat1(WatchListQuery)
  implicit val WatchlistParsedMovieFormat = jsonFormat2(WatchListParsedMovie)
  implicit val WatchlistResultFormat = jsonFormat1(WatchListMovies)

  implicit val UserAccountFormat = jsonFormat3(user.UserAccount.apply)
  implicit val UserFormat = jsonFormat1(user.User.apply)
}

object DockedServer extends App with SimpleRoutingApp {
  // setup
  implicit val actorSystem = ActorSystem()
  implicit val timeout = Timeout(60.second)
  val logger = Logger(LoggerFactory.getLogger("default"))
  import mvgk.httpservice.DockedServer.actorSystem.dispatcher

  // scheduling mvgk.mailer
  val mailer = new Mailer()
  actorSystem.scheduler.schedule(1.minute, 2.hour)(mailer.processWatchLists())

  // creating firefox instance
//  val firefoxDriver: FirefoxDriver = initFirefoxDriver()

  val movieQueryActor = actorSystem.actorOf(Props(new QueryActor(None)).
    withRouter(RoundRobinRouter(10)), "moviequery")

  def initFirefoxDriver(): FirefoxDriver = {
    val firefoxDriver: FirefoxDriver = new FirefoxDriver

    val url = "http://rutracker.org/forum/index.php"

    firefoxDriver.get(url)

    firefoxDriver.findElementByName("login_username").sendKeys("Greg89754")
    firefoxDriver.findElementByName("login_password").sendKeys("parol123")
    firefoxDriver.findElementByName("login").click()

    firefoxDriver
  }

  startServer(interface = "0.0.0.0", port = 8080) {
    import mvgk.httpservice.JsonSupport._

    path("user") {
      get {  // read
        complete(user.User.user)
      } ~
      post { // update
        entity(as[mvgk.user.UserAccount]) {
          userAccount => {
            user.User.user.updateAccounts(userAccount)
            complete("OK")
          }
        }
      }
    } ~
    path("search" / "rutracker") {
      post {
        entity(as[RutrackerQuery]) { query =>
          val result: Future[MovieQueryResult] =
            (movieQueryActor ? Query(query)).mapTo[MovieQueryResult]

          complete(result)
        }
      }
    } ~
    path("search" / "kickass") {
      post {
        entity(as[KickassQuery]) { query: MovieQuery =>
          val result: Future[MovieQueryResult] =
            (movieQueryActor ? Query(query))(180 seconds).mapTo[MovieQueryResult]

          complete(result)
        }
      }
    } ~
    path("search" / "afisha") {
      post {
        entity(as[AfishaQuery]) { query =>
          val result: Future[MovieQueryResult] =
            (movieQueryActor ? Query(query)).mapTo[MovieQueryResult]

          complete(result)
        }
      }
    } ~
    path("watchlist" / "imdb") {
      post {
        entity(as[WatchListQuery]) { query =>
          val result: Future[WatchListMovies] = new Parser().parseEnTitlesByMetaCached(query.link)

          result.value match {
            case Some(Success(x)) => complete(x)
            case None => complete(202, "pending")
            case _ => throw new Exception("watchlist imdb parsing error")
          }
        }
      }
    } ~
    path("process-wl") {
      get {
        respondWithMediaType(MediaTypes.`text/plain`) {
          entity(as[HttpRequest]) {
            obj =>
              logger.info("Handler process-wl called..")

              val f = Future {
                mailer.processWatchLists()
              }

              complete { "OK" }
          }
        }
      }
    } ~
    path("ping") {
      get {
        complete("Up")
      }
    } ~
    path("version") {
      get {
        complete(sbtbuildinfo.BuildInfo.version)
      }
    }
  }
}
