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
    val client = HttpClient()
    val future = client.send(request)

    val response = Await.result(future, 10 seconds)

    assert(response.status == 200)
    assert(new String(response.body, "utf8").contains("This was a get request."))
    assert(err.size == 0)
  }

  test("the POST flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "post"
    builder + ("key", "val")
    val request = builder.post
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a post request."))
    assert(err.size == 0)
  }

  test("the other POST flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "post"
    val request = builder.post("test data")
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a post request."))
    assert(err.size == 0)
  }

  test("the PUT flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "put"
    builder + ("key", "val")
    val request = builder.put
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a put request."))
    assert(err.size == 0)
  }

  test("the other PUT flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "put"
    val request = builder.put("test data")
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a put request."))
    assert(err.size == 0)
  }

  test("the DELETE flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "delete"
    val request = builder.delete
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").contains("This was a delete request."))
    assert(err.size == 0)
  }

  test("the HEAD flow") {
    assert(err.size == 0)
    import DSL._

    val builder:DSL = "http://localhost:3000"
    builder / "head"
    val request = builder.head
    val client = HttpClient()
    val future = client.send(request)

    val res = Await.result(future, 10 seconds)

    assert(res.status == 200)
    assert(new String(res.body, "utf8").isEmpty)
    assert(err.size == 0)
  }

  // TODO replace node.js with a more native "mock"

  test("multiple connections") {
    import DSL._

    val localhost:DSL = "http://localhost:3000"
    val localhostQuery = localhost / "get" get

    val dn:DSL = "http://www.dn.se"
    val dnQuery = dn get

    val client = HttpClient()

    val localFut = client send localhostQuery
    val dnFut = client send dnQuery

    val localRes = Await.result(localFut, 10 seconds)
    val dnRes = Await.result(dnFut, 10 seconds)

    assert(localRes.status == 200)
    assert(err.size == 0)
    assert(dnRes.status == 200)
    assert(new String(dnRes.body, "UTF-8").contains("</body>"))
  }

  test("loads of mixed request with mixed responses") {
    val getUrl:DSL = "http://localhost:3000"
    val postUrl:DSL = "http://localhost:3000"
    val headUrl:DSL = "http://localhost:3000"
    val getReq = getUrl / "get" get
    val postReq = postUrl / "post" post("test data")
    val headReq = headUrl / "head" head
    val client = HttpClient()

    assert(err.size == 0)

    val p1 = client send postReq
    val g1 = client send getReq
    val h1 = client send headReq
    val p2 = client send postReq
    val g2 = client send getReq
    val h2 = client send headReq
    val p3 = client send postReq
    val g3 = client send getReq
    val h3 = client send headReq
    val p4 = client send postReq
    val g4 = client send getReq
    val h4 = client send headReq

    assert(err.size == 0)

    val futures = Array(p1, g1, h1, p2, g2, h2, p3, g3, h3, p4, g4, h4)

    futures.foreach { future =>
      assert(Await.result(future, 10 seconds) != null)
    }
  }

  test("a request with some latency") {
    val host:DSL = "http://localhost:3000"
    val request = host / "slow" get
    val client = HttpClient()

    val resp = client.send(request)

    val body = Await.result(resp, 10 seconds)

    assert(new String(body.body, "utf8").contains("sorry"))
    assert(err.size == 0)
  }

  test("chunked responses") {
    val host:DSL = "http://localhost:3000"
    val req = host / "chunked" get
    val client = HttpClient()

    val fut = client send req
    val resp = Await.result(fut, 10 seconds)

    assert(resp.status == 200)
    assert(new String(resp.body, "UTF-8").contains("chunk1"))
    assert(new String(resp.body, "UTF-8").contains("chunk2"))
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
