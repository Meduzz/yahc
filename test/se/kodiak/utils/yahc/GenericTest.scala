package se.kodiak.utils.yahc

import org.scalatest.FunSuite

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

    val post = builder.post // Post request "key=val" body
    allContains(post.path, Seq("query", "test"))
    allContains(post.postData, Seq("username=test", "password=secret"))
    assert(existsInList("Content-Length", post.headers))
    assert(existsInList("Content-Encoding", post.headers))
    assert(post.headers(1).contains("Dummy"))

    val post2 = builder.post("test") // Post request with "flat" body.
    allContains(post2.path, Seq("query", "test"))
    assert(post2.postData == "test")
    assert(existsInList("Content-Length", post2.headers))
    assert(post2.headers(1).contains("Dummy"))

    val put = builder.put // see post.
    allContains(put.path, Seq("query", "test"))
    allContains(put.putData, Seq("username=test", "password=secret"))
    assert(existsInList("Content-Length", put.headers))
    assert(existsInList("Content-Encoding", put.headers))
    assert(put.headers(1).contains("Dummy"))

    val put2 = builder.put("test") // see post.
    allContains(put2.path, Seq("query", "test"))
    assert(put2.putData == "test")
    assert(existsInList("Content-Length", put2.headers))
    assert(put2.headers(1).contains("Dummy"))

    val delete = builder.delete
    allContains(delete.path, Seq("query", "test"))
    assert(delete.headers(1).contains("Dummy"))

    val head = builder.head
    allContains(head.path, Seq("query", "test"))
    assert(head.headers(1).contains("Dummy"))
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
}
