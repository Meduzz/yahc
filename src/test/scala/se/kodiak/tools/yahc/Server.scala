package se.kodiak.tools.yahc

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.cameltow.Cameltow
import se.chimps.cameltow.framework.Encoded
import se.chimps.cameltow.framework.handlers.Action
import se.chimps.cameltow.framework.responsebuilders.{BadRequest, Error, Ok}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

trait Server extends BeforeAndAfterAll { self:FunSuite =>

	implicit private val duration = Duration(1L, TimeUnit.SECONDS)

	private val routes = Cameltow.routes()

	routes.GET("/:name/:age", Action.sync(req => {
	  val name = req.pathParam("name")
		val age = req.pathParam("age").toInt

	  Ok.json(s"""{"name":"$name", "age":$age}""")
  }))

	routes.POST("/post/echo", Action(req => {
		req.body match {
			case Encoded(bytes) => bytes.map(Ok.json)
			case _ => Future(Error.text("Not an encoded body."))
		}
	}))

	routes.PUT("/put/echo", Action(req => {
		req.body match {
			case Encoded(bytes) => bytes.map(Ok.json)
			case _ => Future(Error.text("Not an encoded body."))
		}
	}))

	routes.POST("/signed", Action(req => {
		req.body match {
			case Encoded(bytes) => {
				bytes.map(bs => {
					val len = bs.length
					val sig = req.header("Signature") match {
						case Some(s) => s.toInt
						case None => 0
					}

					if (len == sig) {
						Ok.json("{}")
					} else {
						BadRequest.json("{}")
					}
				})
			}
			case _ => Future(Error.text("Not an encoded body."))
		}
	}))

	private val server = Cameltow.blank("test")
  	.handler(routes.handler)
  	.listen(6006)

	override protected def beforeAll():Unit = server.start()

	override protected def afterAll():Unit = server.stop()
}
