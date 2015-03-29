package mvgk.watchlistparser

import com.typesafe.scalalogging.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.caching.LruCache

case class WatchListParsedMovie(title: String, year: Int)
case class WatchListMovies(list: List[WatchListParsedMovie])

case class WatchListQuery(link: String)

/**
 * @author Got Hug
 */
object Parser {
  val cache = LruCache[WatchListMovies](timeToLive = 30.minutes)
}

class Parser {
  val logger = Logger(LoggerFactory.getLogger("mvgk/watchlistparser"))

  def parse(url: String, firefoxDriver: Option[FirefoxDriver] = None) = {
    logger.info("Parsing watchlist..")

    val html = Jsoup.connect(url).timeout(10000).get()

    val imgMovies = html.body().getElementsByClass("lister-list").first().getElementsByTag("img")

    val movieTitles = imgMovies.toArray(new Array[Element](imgMovies.size)).map(_.attr("alt"))

    val yearRe = "[\\d]{4}".r

    val years =
      for {
        elem <- html.body().getElementsByClass("lister-item-year")
        year = yearRe findFirstIn elem.text
      } yield year.get.toInt

    def toEngLetters(str: String) = org.apache.commons.lang3.StringUtils.stripAccents(str)

    val watchListMovies = WatchListMovies((movieTitles zip years).map(x => WatchListParsedMovie(toEngLetters(x._1), x._2)).toList)

    logger.info("Watchlist parsed successfully")

    watchListMovies
  }

  def parseEnTitlesByMetaCached(url: String): Future[WatchListMovies] = Parser.cache(url) {
    Future { parseEnTitlesByMeta(url) }
  }

  def parseEnTitlesByMeta(url: String): WatchListMovies = {
    def parseMovie(url: String): WatchListParsedMovie = {
      val EngText = "[\\x20-\\x7E–]"
      val Year = "\\d{4}"

      val EngtitleYear = s"($EngText+)[\\s]+\\($EngText*?($Year)$EngText*\\)".r

      val movieHtml = Jsoup.connect(url).timeout(10000).get()
      val content = movieHtml.getElementsByAttributeValue("property", "og:title").head.attr("content")

      content match {
        case EngtitleYear(title, year) => WatchListParsedMovie(title, year.toInt)
        case _ =>
          throw new Exception(
            s"Failed to parse English title and year from the string" +
            s" '$content', movie url $url")
      }

      /**
      val movieWidgetHeader =
        movieHtml.getElementById("title-overview-widget").getElementsByClass("header").head
      val originalTitle: Option[String] =
        movieWidgetHeader.getElementsByClass("title-extra").headOption.map(_.ownText)
        **/
    }

    logger.info("Parsing watchlist..")

    val html = Jsoup.connect(url).timeout(10000).get()

    val divTitleList =
      html.body().getElementsByClass("lister-list").first().getElementsByClass("title").toList

    val links = divTitleList.map(_.getElementsByTag("a")).flatMap(_.map(_.absUrl("href")))

    val futures: List[Future[WatchListParsedMovie]] =
      links map {
        link =>
          Future { parseMovie(link) }
      }

    val parsedMovies = Await.result(Future.sequence(futures), 50 seconds)

    WatchListMovies(parsedMovies)
  }
}

object ParserTest extends App {
  val parser = new Parser()

  val url = "http://www.imdb.com/user/ur9112878/watchlist?ref_=wt_nv_wl_all_0"

  val titles = parser.parseEnTitlesByMeta(url)
}
