package se.kodiak.utils.yahc

import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.sys.process._
import scala.concurrent.Await
import scala.concurrent.duration._

class FlowTest extends FunSuite with BeforeAndAfter {
  val logger = ProcessLogger({o => out = out ++ List(o)}, {e => err = err ++ List(e)})
  val process:Process = "/usr/local/bin/node test/se/kodiak/utils/yahc/server.js".run(logger) // TODO extract to property.
  var out = List[String]()
  var err = List[String]()

  before {
    out = List[String]()
    err = List[String]()
  }

  test("the GET flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "get"

    val request = builder.get
    val client = HttpClient(request.server)
    val future = client.send(request)

    val response = Await.result(future, 10 seconds)

    assert(response.status == 200)
    assert(new String(response.body, "utf8").contains("This was a get request."))
  }

  test("the POST flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "post"
    builder + ("key", "val")
    val request = builder.post
    val client = HttpClient(request.server)
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a post request."))
  }

  test("shutdown node") {
    process.destroy()
    info("Done")
  }

  after {
//    println("Node logs: \n"+out.mkString("\n"))
//    println()
//    println("Node errors: \n"+err.mkString("\n"))
  }
}
