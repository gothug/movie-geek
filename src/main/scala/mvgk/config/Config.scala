package mvgk.config

import com.typesafe.config.ConfigFactory
import mvgk.db.DBConfig

/**
 * @author Got Hug
 */
object Config {
  lazy val db = DBConfig.apply(ConfigFactory.load("db"))
}
