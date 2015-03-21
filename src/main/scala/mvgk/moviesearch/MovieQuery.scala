package mvgk.moviesearch

import com.typesafe.scalalogging.slf4j.Logger
import org.openqa.selenium.firefox.FirefoxDriver
import org.slf4j.LoggerFactory

/**
 * @author Got Hug
 */
case class MovieQueryResult(link: Option[String], md5: String)

trait MovieQuery {
  val title: String
  val titleRus: Option[String]
  val year: Int

  def doQuery(firefoxDriver: Option[FirefoxDriver] = None): MovieQueryResult

  def logInfo(info: String) = Logger(LoggerFactory.getLogger(this.getClass.getName)).info(info)
}
