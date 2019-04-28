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

  val dropCounter = new java.util.concurrent.atomic.AtomicInteger(0)
  val insertCounter = new java.util.concurrent.atomic.AtomicInteger(0)

  val scn = scenario("Select Simulation")
    .repeat(1) {
      pause(50 milliseconds)
      .exec(session => session.set("dropCounter", dropCounter.getAndIncrement))
      .doIf(session => session("dropCounter").as[Integer] < 1) {
        exec(http("drop create")
        .get("/runscript")
        .queryParam("resourceUrl", "classpath:/static/test/create-message.sql")
        .check(status.is(200))
      )}
//      .exec(session => {
//        println(session("counter").as[String])
//        session }
//      )
    }
    .rendezVous(nUsers)
    .repeat(1000) {
      pause(20 milliseconds)
      .exec(session => session.set("insertCounter", insertCounter.getAndIncrement))
      .doIf(session => session("insertCounter").as[Integer] < 1000) {
        exec(http("insert messages")
        .post("/update")
        .header(HttpHeaderNames.ContentType, "text/plain")
        .body(StringBody("insert into message(context_id_, text_) values('uuid', 'Message uuid')"))
        .check(status.is(200))
      )}
    }
    .rendezVous(nUsers)
    .repeat(100) {
      pause(500 milliseconds)
      .exec(http("select messages")
        .post("/select")
        .header(HttpHeaderNames.ContentType, "text/plain")
        .body(StringBody("select * from message"))
        .check(status.is(200))
      )
    }

  setUp(
    scn.inject(rampUsers(nUsers) during (30 seconds)
     // .atOnceUsers(nUsers)
    ).protocols(httpConf))
}
