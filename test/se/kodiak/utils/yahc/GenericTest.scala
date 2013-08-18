package se.kodiak.utils.yahc

import org.scalatest.FunSuite
import akka.util.ByteString

class GenericTest extends FunSuite {

  test("dsl does build") {
    import DSL._
    val builder:DSL = "http://www.test.com/path?query=true" // the :DSL are annoyingly needed!
    assert(builder.isInstanceOf[DSL])

    builder ? ("test", "true") // query param ie ?test=true
    builder & ("Dummy", "isSet") // header param ie Dummy: isSet
    builder + ("username", "test") // post/put param
    builder + ("password", "secret") // post/put param

    val get = builder.get // Get request.
    allContains(get.path, Seq("query", "test"))
    assert(get.headers(1).contains("Dummy"))
    info("GET request build ok")

    val post = builder.post // Post request "key=val" body
    allContains(post.path, Seq("query", "test"))
    allContains(post.postData, Seq("username=test", "password=secret"))
    assert(existsInList("Content-Length", post.headers))
    assert(existsInList("Content-Encoding", post.headers))
    assert(post.headers(1).contains("Dummy"))
    info("POST request with formdata build ok")

    val post2 = builder.post("test") // Post request with "flat" body.
    allContains(post2.path, Seq("query", "test"))
    assert(post2.postData == "test")
    assert(existsInList("Content-Length", post2.headers))
    assert(post2.headers(1).contains("Dummy"))
    info("POST request with plain data build ok")

    val put = builder.put // see post.
    allContains(put.path, Seq("query", "test"))
    allContains(put.putData, Seq("username=test", "password=secret"))
    assert(existsInList("Content-Length", put.headers))
    assert(existsInList("Content-Encoding", put.headers))
    assert(put.headers(1).contains("Dummy"))
    info("PUT request with formdata build ok")

    val put2 = builder.put("test") // see post.
    allContains(put2.path, Seq("query", "test"))
    assert(put2.putData == "test")
    assert(existsInList("Content-Length", put2.headers))
    assert(put2.headers(1).contains("Dummy"))
    info("PUT request with plain data build ok")

    val delete = builder.delete
    allContains(delete.path, Seq("query", "test"))
    assert(delete.headers(1).contains("Dummy"))
    info("DELETE request build ok")

    val head = builder.head
    allContains(head.path, Seq("query", "test"))
    assert(head.headers(1).contains("Dummy"))
    info("HEAD request build ok")
  }

  test("no query no questionmark") {
    import DSL._

    val builder:DSL = "http://www.test.com"
    builder / "path" / "to" / "destruction"

    val req = builder.get

    assert(req.path.contains("/path/to/destruction"))
    assert(!req.path.contains("?"))
  }

  def allContains(target:String, subjects:Seq[String]) {
    subjects.foreach { subject =>
      assert(target.contains(subject))
    }
  }

  def existsInList(subject:String, targets:List[String]):Boolean = {
    targets.filter { t =>
      t.contains(subject)
    }.length > 0
  }

  val get = "GET /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\n\r\n"
  val post = "POST /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\nContent-Length:9\r\nContent-Encoding:application/x-www-form-urlencoded\r\n\r\nbody=test"
  val post2 = "POST /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\nContent-Length:10\r\n\r\nDin mamma!"
  val put = "PUT /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\nContent-Length:9\r\nContent-Encoding:application/x-www-form-urlencoded\r\n\r\nbody=test"
  val put2 = "PUT /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\nContent-Length:10\r\n\r\nDin mamma!"
  val delete = "DELETE /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\n\r\n"
  val head = "HEAD /path?query=test HTTP/1.1\r\nHost:www.test.com\r\nheader:test\r\n\r\n"

  test("HttpUtil builds correct request") {
    import DSL._
    val dsl:DSL = "http://www.test.com/path?query=test"
    dsl & ("header", "test")
    dsl + ("body", "test")

    val utilGet = HttpUtil(dsl.get)
    assert(utilGet.equals(ByteString.fromString(get, "UTF-8")))
    info("GET response parsed ok")

    val utilPost = HttpUtil(dsl.post)
    assert(utilPost.equals(ByteString.fromString(post, "UTF-8")))
    info("POST response parsed ok")

    val utilPost2 = HttpUtil(dsl.post("Din mamma!"))
    assert(utilPost2.equals(ByteString.fromString(post2, "UTF-8")))
    info("POST response plain parsed ok")

    val utilPut = HttpUtil(dsl.put)
    assert(utilPut.equals(ByteString.fromString(put, "UTF-8")))
    info("PUT response parsed ok")

    val utilPut2 = HttpUtil(dsl.put("Din mamma!"))
    assert(utilPut2.equals(ByteString.fromString(put2, "UTF-8")))
    info("PUT response with plain data ok")

    val utilDelete = HttpUtil(dsl.delete)
    assert(utilDelete.equals(ByteString.fromString(delete, "UTF-8")))
    info("DELETE response parsed ok")

    val utilHead = HttpUtil(dsl.head)
    assert(utilHead.equals(ByteString.fromString(head, "UTF-8")))
    info("HEAD response parsed ok")
  }

  test("HttpUtil builds correct responses") {
    val bodylessResponse = "HTTP/1.1 200 OK\r\nheader:test\r\nmessage:i fell for it\r\n\r\n"
    val bodyfulResponse = "HTTP/1.1 200 OK\r\nheader:test\r\nContent-Encoding:UTF-8\r\nContent-Length:23\r\n\r\nDetta kunde varit HTML!\r\n\r\n"
    val longbodyResponse = "HTTP/1.1 200 OK\r\nheader:test\r\nContent-Encoding:UTF-8\r\nContent-Length:164\r\n\r\nI need to break the 128 bit \"read\" limit, so I'll just go on here for a while, and see if this crashes or not! Ok, apparently I have to go one for a while longer ;)\r\n\r\n"

    val headUtil = HttpUtil(ByteString.fromString(bodylessResponse, "UTF-8"))
    assert(headUtil.status == 200)
    assert(headUtil.body.length == 0)
    assert(headUtil.headers("header").equals("test"))
    info("No body response parsed ok")

    val shortBodyUtil = HttpUtil(ByteString.fromString(bodyfulResponse, "UTF-8"))
    assert(shortBodyUtil.status == 200)
    assert(shortBodyUtil.body.length == 23)
    assert(shortBodyUtil.headers.size == 3)
    assert(shortBodyUtil.headers("Content-Length").equals("23"))
    info("short body response parsed ok")

    val longBodyUtil = HttpUtil(ByteString.fromString(longbodyResponse, "UTF-8"))
    assert(longBodyUtil.status == 200)
    assert(longBodyUtil.body.length == 164)
    assert(longBodyUtil.headers.size == 3)
    assert(longBodyUtil.headers("Content-Length").equals("164"))
    info("Long body response parsed ok")
  }
}
