package mvgk

/**
 * @author Got Hug
 */
package object util {
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
}
