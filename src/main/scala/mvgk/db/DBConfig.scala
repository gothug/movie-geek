package mvgk.db

/**
 * @author Got Hug
 */
case class DBConfig(name: String,
                    user: String,
                    password: String,
                    driver: String)

object DBConfig {
  def apply(config: com.typesafe.config.Config): DBConfig = {
    import config._

    DBConfig(
      name = getString("db.name"),
      user = getString("db.user"),
      password = getString("db.password"),
      driver = getString("db.driver"))
  }
}
