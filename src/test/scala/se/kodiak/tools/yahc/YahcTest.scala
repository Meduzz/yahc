package se.kodiak.tools.yahc

import org.scalatest.{FunSuite, Matchers}
import se.kodiak.tools.yahc.Yahc._

class YahcTest extends FunSuite with Matchers with Server {

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

	test("silly signing - happy version") {
		val origin = Test("asdf", 73)
		val result = POST("http://localhost:6006/signed", origin, Some((b, r) => {
			r.header("Signature", s"${b.length}")
		})).asString

		result.code shouldBe 200
	}

	test("silly signing - unhappy version") {
		val origin = Test("asdf", 73)
		val result = POST("http://localhost:6006/signed", origin, Some((b, r) => {
			r.header("Signature", s"${-1}")
		})).asString

		result.code shouldBe 400
	}
}

case class Test(name:String, age:Int)