package se.kodiak.tools.yahc

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{FunSuite, Matchers}
import se.kodiak.tools.yahc.Yahc._

import scala.concurrent.ExecutionContext.Implicits.global

class YahcTest extends FunSuite with ScalaFutures with Matchers with Server {

	override implicit def patienceConfig = PatienceConfig(Span(500, Milliseconds), Span(50, Milliseconds))

	test("get / test / 16") {
		val result = GET("http://localhost:6006/test/16").asString
	  	.asJson[Test]

		result.name shouldBe "test"
		result.age shouldBe 16
	}

	test("post / echo") {
		val origin = Test("asdf", 10)
		val result = POST("http://localhost:6006/post/echo", origin).asBytes
			.asJson[Test]

		result.name shouldBe origin.name
		result.age shouldBe origin.age
	}

	test("put / echo") {
		val origin = Test("asdf", 10)
		val result = PUT("http://localhost:6006/put/echo", origin).asString
			.asJson[Test]

		result.name shouldBe origin.name
		result.age shouldBe origin.age
	}
}

case class Test(name:String, age:Int)