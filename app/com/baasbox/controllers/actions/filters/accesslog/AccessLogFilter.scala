package com.baasbox.filters

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j._
import java.util.Date
import com.baasbox.BBConfiguration
import scala.concurrent.Future


class LoggingFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])(rh: RequestHeader): Future[SimpleResult] = {
    val startTime = System.currentTimeMillis
    nextFilter(rh).map { result =>
      if(BBConfiguration.getWriteAccessLog()) {
        val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
        val time = System.currentTimeMillis - startTime
        val dateFormatted = new Date(startTime)
        val userAgent = rh.headers.get("User-Agent").getOrElse("")
        val contentLength = result.header.headers.get("Content-Length").getOrElse("-")
        /*
        * Log format is the combined one: http://httpd.apache.org/docs/2.2/logs.html
        * Unfortunely we have to do a litlle hack to log the authenticated username due a limitation of the framework: scala cannot access to the current Http Context where the username is stored
        */
        val username = result.header.headers.get("BB-USERNAME").getOrElse("-")
        result.withHeaders("BB-USERNAME"->"")
        filterLogger.info(s"""${rh.remoteAddress}\t-\t${username}\t[${dateFormatted}]\t${"\""}${rh.method} ${rh.uri} ${rh.version}${"\""}\t${result.header.status}\t${contentLength}\t${"\""}${"\""}\t${"\""}${userAgent}${"\""}\t${time}""")
      }
      result
    }
  }
}
