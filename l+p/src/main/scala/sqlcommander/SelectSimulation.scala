package sqlcommander

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class SelectSimulation extends Simulation {
  val nUsers = Integer.getInteger("users", 20)
  val uri = sys.props.getOrElse("uri", "http://localhost:11515")
  val httpConf = http
    .disableWarmUp
    .baseUrl(uri)
    .acceptHeader("application/json;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Java/1.8.0_131")
    .contentTypeHeader(HttpHeaderValues.TextXml)

  val scn = scenario("Select Simulation")
    .repeat(100) {
      pause(300 milliseconds)
      .exec(http("select messages")
        .post("/select")
        .header(HttpHeaderNames.ContentType, "text/plain")
        .body(StringBody("select * from message"))
        .check(status.is(200))
      )
    }

  setUp(
    scn.inject(
      atOnceUsers(nUsers)
    ).protocols(httpConf))
}
