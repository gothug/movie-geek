package mvgk

import scala.annotation.tailrec

/**
 * @author Got Hug
 */
package object util {
  private val DefaultDelay = 100

  def getMd5(s: String) = {
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = s.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  def getEnvVar(variable: String, defaultVal: String) = {
    if (sys.env.isDefinedAt(variable)) {
      sys.env(variable)
    } else {
      defaultVal
    }
  }

  @tailrec
  def retry[T](timeout: Long, delay: Long = DefaultDelay)(fn: => T): T = {
    try {
      fn
    } catch {
      case e: Throwable =>
        if (timeout <= 0) {
          throw e
        } else {
          Thread.sleep(delay)
          retry(timeout - delay, delay)(fn)
        }
    }
  }
}
