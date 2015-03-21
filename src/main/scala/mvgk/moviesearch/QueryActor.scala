package mvgk.moviesearch

import akka.actor.Actor
import org.openqa.selenium.firefox.FirefoxDriver

/**
 * @author Got Hug
 */
object QueryActor {
  case class Query(query: MovieQuery)
}

class QueryActor(firefoxDriver: Option[FirefoxDriver] = None) extends Actor {
  import mvgk.moviesearch.QueryActor._

  override def receive = {
    case Query(query) => sender ! query.doQuery(firefoxDriver)
  }
}
